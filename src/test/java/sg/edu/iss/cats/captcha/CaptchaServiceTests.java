package sg.edu.iss.cats.captcha;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class CaptchaServiceTests {
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2030-01-01T12:00:00Z");
        void advance(long milliseconds) { now = now.plusMillis(milliseconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test void iconPuzzleIsServerValidatedSingleUseAndCannotBeReplayed() throws Exception {
        var clock = new MutableClock();
        var service = new CaptchaService(clock);
        var session = new MockHttpSession();
        var puzzle = service.issueForKind(session, "login-staff", CaptchaService.Kind.ICON_ORDER);
        assertThat(puzzle.choices()).hasSize(9);
        assertThat(puzzle.boardUrl()).contains("part=board");
        assertThat(puzzle.choices()).extracting(CaptchaService.IconChoice::slot)
                .containsExactly(0,1,2,3,4,5,6,7,8);
        var image = service.media(session, puzzle.id(), "board");
        assertThat(ImageIO.read(new ByteArrayInputStream(image)).getWidth()).isEqualTo(340);
        assertThat(puzzle.choices()).extracting(CaptchaService.IconChoice::id).doesNotHaveDuplicates();
        String correct = expectedAnswer(session, "login-staff");
        clock.advance(500);
        assertThat(service.verify(session, "login-staff", puzzle.id(), correct)).isTrue();
        assertThat(service.verify(session, "login-staff", puzzle.id(), correct)).isFalse();
    }

    @Test void rotatingCatMustFaceFishAndAnswerExpires() throws Exception {
        var clock = new MutableClock();
        var service = new CaptchaService(clock);
        var session = new MockHttpSession();
        var puzzle = service.issueForKind(session, "mfa-challenge", CaptchaService.Kind.ROTATE_TO_FOOD);
        assertThat(puzzle.boardUrl()).contains("part=board");
        assertThat(puzzle.pieceUrl()).contains("part=piece");
        assertThat(ImageIO.read(new ByteArrayInputStream(service.media(session,puzzle.id(),"board"))).getWidth()).isEqualTo(300);
        String answer = expectedAnswer(session, "mfa-challenge");
        assertThat(answer).isNotEqualTo("0");
        clock.advance(450);
        assertThat(service.verify(session, "mfa-challenge", puzzle.id(), answer)).isTrue();
        var expired = service.issueForKind(session, "mfa-challenge", CaptchaService.Kind.ROTATE_TO_FOOD);
        clock.advance(200_000);
        assertThat(service.verify(session, "mfa-challenge", expired.id(), "1")).isFalse();
    }

    @Test void matchingCatChallengeHasOpaqueChoicesAndAcceptsAnySelectionOrder() throws Exception {
        var clock = new MutableClock();
        var service = new CaptchaService(clock);
        var session = new MockHttpSession();
        var puzzle = service.issueForKind(session, "fee-claim", CaptchaService.Kind.MATCHING_CATS);
        assertThat(puzzle.kind()).isEqualTo("MATCHING_CATS");
        assertThat(puzzle.choices()).hasSize(9);
        assertThat(puzzle.boardUrl()).contains("part=board");
        assertThat(puzzle.pieceUrl()).isNull();
        assertThat(puzzle.toString()).doesNotContain("adventure", "celebrate", "confident");
        var board = service.media(session, puzzle.id(), "board");
        assertThat(ImageIO.read(new ByteArrayInputStream(board)).getWidth()).isEqualTo(340);
        assertThat(ImageIO.read(new ByteArrayInputStream(board)).getHeight()).isEqualTo(318);
        assertThat(service.media(session, puzzle.id(), "piece")).isNull();
        String[] correct = expectedAnswer(session, "fee-claim").split(",");
        assertThat(correct).hasSize(3);
        clock.advance(800);
        // Matching, unlike ordered icons, is insensitive to the order of taps.
        String reversed = String.join(",", correct[2], correct[0], correct[1]);
        assertThat(service.verify(session, "fee-claim", puzzle.id(), reversed)).isTrue();
        assertThat(service.verify(session, "fee-claim", puzzle.id(), reversed)).isFalse();
        assertThat(service.media(session, puzzle.id(), "board")).isNull();
    }

    @Test void matchingCatChallengeRejectsRepeatedOrIncorrectTokens() throws Exception {
        var clock = new MutableClock();
        var service = new CaptchaService(clock);
        var session = new MockHttpSession();
        var puzzle = service.issueForKind(session, "fee-claim", CaptchaService.Kind.MATCHING_CATS);
        var correct = expectedAnswer(session, "fee-claim").split(",");
        clock.advance(800);
        assertThat(service.verify(session, "fee-claim", puzzle.id(),
                String.join(",", correct[0], correct[0], correct[1]))).isFalse();
        // Incorrect answer consumes challenge, so the correct one can't be replayed.
        assertThat(service.verify(session, "fee-claim", puzzle.id(),
                String.join(",", correct))).isFalse();
        puzzle = service.issueForKind(session, "fee-claim", CaptchaService.Kind.MATCHING_CATS);
        clock.advance(800);
        assertThat(service.verify(session, "fee-claim", puzzle.id(), "invalid,invalid,invalid")).isFalse();
    }

    @Test void captchaCannotBeSubmittedImmediatelyOrWithAnotherId() throws Exception {
        var clock = new MutableClock();
        var service = new CaptchaService(clock);
        var session = new MockHttpSession();
        var puzzle = service.issueForKind(session, "login-admin", CaptchaService.Kind.ROTATE_TO_FOOD);
        String answer = expectedAnswer(session, "login-admin");
        assertThat(service.verify(session, "login-admin", puzzle.id(), answer)).isFalse();
        clock.advance(500);
        assertThat(service.verify(session, "login-admin", puzzle.id(), answer)).isFalse();
        var next = service.issueForKind(session, "login-admin", CaptchaService.Kind.ROTATE_TO_FOOD);
        clock.advance(500);
        assertThat(service.verify(session, "login-admin", "not-the-id", "1")).isFalse();
        assertThat(service.verify(session, "login-admin", next.id(), "1")).isFalse();
    }

    private String expectedAnswer(MockHttpSession session, String scope) throws Exception {
        Field key = CaptchaService.class.getDeclaredField("SESSION_KEY");
        key.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (Map<String, ?>) session.getAttribute((String) key.get(null));
        var stored = map.get(scope);
        Field answer = stored.getClass().getDeclaredField("solution");
        answer.setAccessible(true);
        return (String) answer.get(stored);
    }

    @Test void refreshCyclesAcrossAllThreePuzzleTypes() {
        var service = new CaptchaService();
        var session = new MockHttpSession();
        var kinds = java.util.Set.of(service.issue(session, "login-staff").kind(),
                service.issue(session, "login-staff").kind(),
                service.issue(session, "login-staff").kind());
        assertThat(kinds).containsExactlyInAnyOrder("ICON_ORDER", "ROTATE_TO_FOOD", "MATCHING_CATS");
    }
}
