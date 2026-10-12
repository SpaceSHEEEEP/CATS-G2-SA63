package sg.edu.iss.cats.captcha;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    public enum Kind { ICON_ORDER, ROTATE_TO_FOOD, MATCHING_CATS }

    private static final String SESSION_KEY = CaptchaService.class.getName() + ".challenges";
    private static final String LAST_KIND_KEY = CaptchaService.class.getName() + ".lastKind";
    private static final String ISSUE_WINDOW_KEY = CaptchaService.class.getName() + ".issueWindow";
    private static final int MAX_ISSUES_PER_MINUTE = 24;

    private record IssueWindow(long startedAt, int count) {}
    private static final long MAX_AGE_MS = 180_000;
    private static final long MIN_SOLVE_MS = 350;
    // MFA enrollment and TOTP verification intentionally have no CAPTCHA.
    private static final Set<String> SCOPES = Set.of("login-staff", "login-admin",
            "pair-code", "fee-claim", "admin-mail");
    private static final List<String> CATS = List.of("adventure", "celebrate", "confident", "curious",
            "excited", "surprised", "tearful", "thoughtful", "unimpressed");
    private static final List<String> ICONS = List.of("star", "fish", "heart", "moon", "bell", "balloon", "paw", "flower", "diamond");
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public CaptchaService() { this(Clock.systemUTC()); }
    CaptchaService(Clock clock) { this.clock = clock; }

    /** Only rendered artwork and opaque hitbox IDs leave the server. */
    public record ChallengeView(String id, String kind, String prompt,
            List<IconChoice> choices, String boardUrl, String pieceUrl) {}
    public record IconChoice(String id, int slot) {}

    private static final class Challenge {
        final String id;
        final Kind kind;
        final long issued;
        final String solution;
        final byte[] board;
        final byte[] piece;
        final int[] iconX, iconY, iconSizes;
        Challenge(String id, Kind kind, long issued, String solution, byte[] board, byte[] piece) {
            this(id, kind, issued, solution, board, piece, null, null, null);
        }
        Challenge(String id, Kind kind, long issued, String solution, byte[] board, byte[] piece,
                int[] iconX, int[] iconY, int[] iconSizes) {
            this.id = id; this.kind = kind; this.issued = issued; this.solution = solution;
            this.board = board; this.piece = piece;
            this.iconX = iconX; this.iconY = iconY; this.iconSizes = iconSizes;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Challenge> challenges(HttpSession session) {
        synchronized (session) {
            Object old = session.getAttribute(SESSION_KEY);
            if (old instanceof Map<?, ?>) return (Map<String, Challenge>) old;
            Map<String, Challenge> map = new ConcurrentHashMap<>();
            session.setAttribute(SESSION_KEY, map);
            return map;
        }
    }

    public boolean isSupportedScope(String scope) { return SCOPES.contains(scope); }

    public ChallengeView issue(HttpSession session, String scope) {
        if (session == null) throw new IllegalArgumentException("Session required");
        Kind kind;
        synchronized (session) {
            IssueWindow window = (IssueWindow) session.getAttribute(ISSUE_WINDOW_KEY);
            long now = clock.millis();
            if (window == null || now - window.startedAt() >= 60_000 || now < window.startedAt())
                window = new IssueWindow(now, 0);
            if (window.count() >= MAX_ISSUES_PER_MINUTE)
                throw new TooManyChallengesException();
            session.setAttribute(ISSUE_WINDOW_KEY, new IssueWindow(window.startedAt(), window.count() + 1));
            Integer previous = (Integer) session.getAttribute(LAST_KIND_KEY);
            int next = previous == null ? random.nextInt(Kind.values().length)
                    : (previous + 1) % Kind.values().length;
            session.setAttribute(LAST_KIND_KEY, next);
            kind = Kind.values()[next];
        }
        return issueForKind(session, scope, kind);
    }

    public static class TooManyChallengesException extends RuntimeException {
        public TooManyChallengesException() { super("Too many puzzles requested. Try again in a minute."); }
    }

    ChallengeView issueForKind(HttpSession session, String scope, Kind kind) {
        if (!isSupportedScope(scope)) throw new IllegalArgumentException("Unknown CAPTCHA form");
        if (session == null) throw new IllegalArgumentException("Session required");
        String id = UUID.randomUUID().toString();
        String cat = CATS.get(random.nextInt(CATS.size()));
        ChallengeView view;
        Challenge challenge;
        long now = clock.millis();
        String boardUrl = "/captcha/media?id=" + id + "&part=board";
        String pieceUrl = "/captcha/media?id=" + id + "&part=piece";
        if (kind == Kind.ICON_ORDER) {
            List<String> icons = new ArrayList<>(ICONS);
            Collections.shuffle(icons, random);
            int[] angles = new int[9], sizes = new int[9], xs = new int[9], ys = new int[9];
            for (int i = 0; i < 9; i++) {
                angles[i] = random.nextInt(71) - 35;
                sizes[i] = 27 + random.nextInt(23);
            }
            scatterIcons(sizes, xs, ys);
            List<String> sequence = icons.subList(0, 3);
            try {
                challenge = new Challenge(id, kind, now, "0,1,2",
                        CaptchaArt.orderedIconBoard(cat, icons.toArray(String[]::new),
                                angles, sizes, xs, ys), null, xs, ys, sizes);
            } catch (IOException ex) { throw new IllegalStateException("Cannot create visual verification", ex); }
            // Never disclose the hitboxes or icon-to-position mapping to the browser.
            view = new ChallengeView(id, kind.name(),
                    "Tap these icons in order: " + String.join(" → ", sequence),
                    List.of(), boardUrl, null);
        } else if (kind == Kind.ROTATE_TO_FOOD) {
            int foodSide = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left
            int start = random.nextInt(4);
            if (start == foodSide) start = (start + 1 + random.nextInt(3)) % 4;
            int turns = (foodSide - start + 4) % 4;
            List<String> decoys = new ArrayList<>(ICONS);
            decoys.remove("fish");
            Collections.shuffle(decoys, random);
            try {
                byte[][] images = CaptchaArt.rotationBoards(cat, start * 90, foodSide,
                        decoys.subList(0, 3).toArray(String[]::new));
                challenge = new Challenge(id, kind, now, Integer.toString(turns), images[0], images[1]);
            } catch (IOException ex) { throw new IllegalStateException("Cannot create visual verification", ex); }
            view = new ChallengeView(id, kind.name(),
                    "Turn the cat until its head faces the fish, not the other objects.",
                    null, boardUrl, pieceUrl);
        } else {
            List<String> distractors = new ArrayList<>(CATS);
            distractors.remove(cat);
            Collections.shuffle(distractors, random);
            List<String> panelCats = new ArrayList<>(distractors.subList(0, 6));
            panelCats.addAll(List.of(cat, cat, cat));
            Collections.shuffle(panelCats, random);
            List<IconChoice> choices = new ArrayList<>();
            List<String> selectedTokens = new ArrayList<>();
            int[] angles = new int[9];
            int[] scales = new int[9];
            int[] backgrounds = new int[9];
            boolean[] mirrored = new boolean[9];
            for (int i = 0; i < 9; i++) {
                String token = UUID.randomUUID().toString();
                choices.add(new IconChoice(token, i));
                if (panelCats.get(i).equals(cat)) selectedTokens.add(token);
                angles[i] = random.nextInt(31) - 15;
                scales[i] = 86 + random.nextInt(24);
                backgrounds[i] = random.nextInt(6);
                mirrored[i] = random.nextBoolean();
            }
            Collections.sort(selectedTokens);
            String answer = String.join(",", selectedTokens);
            try {
                challenge = new Challenge(id, kind, now, answer,
                        CaptchaArt.matchingCatsBoard(panelCats.toArray(String[]::new),
                                angles, scales, backgrounds, mirrored), null);
            } catch (IOException ex) { throw new IllegalStateException("Cannot create visual verification", ex); }
            view = new ChallengeView(id, kind.name(),
                    "Find the three panels showing the same cat (ignore their rotation and backgrounds).",
                    choices, boardUrl, null);
        }
        challenges(session).put(scope, challenge);
        return view;
    }

    private void scatterIcons(int[] sizes, int[] xs, int[] ys) {
        // Rejection sampling creates a real scattered arrangement, not a disguised grid.
        for (int round = 0; round < 40; round++) {
            boolean allPlaced = true;
            for (int i = 0; i < sizes.length; i++) {
                boolean placed = false;
                for (int attempt = 0; attempt < 2500 && !placed; attempt++) {
                    int x = 31 + random.nextInt(278), y = 29 + random.nextInt(197);
                    placed = true;
                    for (int j = 0; j < i; j++) {
                        int separation = (sizes[i] + sizes[j]) / 2 + 14;
                        int dx = x - xs[j], dy = y - ys[j];
                        if (dx * dx + dy * dy < separation * separation) {
                            placed = false; break;
                        }
                    }
                    if (placed) { xs[i] = x; ys[i] = y; }
                }
                if (!placed) { allPlaced = false; break; }
            }
            if (allPlaced) return;
        }
        throw new IllegalStateException("Cannot arrange CAPTCHA symbols");
    }

    private static boolean orderedClicksMatch(Challenge stored, String answer) {
        String[] positions = answer.split(",", -1);
        if (positions.length != 3) return false;
        String[] targetOrder = stored.solution.split(",");
        for (int i = 0; i < 3; i++) {
            String[] components = positions[i].split(":", -1);
            if (components.length != 2 || !components[0].matches("[0-9]{1,4}")
                    || !components[1].matches("[0-9]{1,4}")) return false;
            int nx = Integer.parseInt(components[0]), ny = Integer.parseInt(components[1]);
            if (nx > 1000 || ny > 1000) return false;
            int x = Math.round(nx * 340f / 1000), y = Math.round(ny * 255f / 1000);
            int target = Integer.parseInt(targetOrder[i]);
            int dx = x - stored.iconX[target], dy = y - stored.iconY[target];
            int radius = stored.iconSizes[target] / 2 + 5;
            if (dx * dx + dy * dy > radius * radius) return false;
        }
        return true;
    }

    public boolean verify(HttpSession session, String scope, String id, String answer) {
        if (session == null || !isSupportedScope(scope) || id == null || answer == null
                || id.length() > 64 || answer.length() > 128) return false;
        Challenge stored = challenges(session).remove(scope);
        if (stored == null) return false;
        long elapsed = clock.millis() - stored.issued;
        if (!stored.id.equals(id) || elapsed < MIN_SOLVE_MS || elapsed > MAX_AGE_MS) return false;
        if (stored.kind == Kind.ICON_ORDER) return orderedClicksMatch(stored, answer);
        if (stored.kind == Kind.MATCHING_CATS) {
            String[] tokens = answer.split(",", -1);
            if (tokens.length != 3 || !java.util.Arrays.stream(tokens)
                    .allMatch(token -> token.matches("[a-f0-9-]{36}"))
                    || java.util.Arrays.stream(tokens).distinct().count() != 3) return false;
            java.util.Arrays.sort(tokens);
            return stored.solution.equals(String.join(",", tokens));
        }
        return stored.solution.equals(answer);
    }

    public byte[] media(HttpSession session, String id, String part) {
        if (session == null || id == null || !id.matches("[a-f0-9-]{36}")) return null;
        long now = clock.millis();
        for (Challenge c : challenges(session).values()) {
            if (c.id.equals(id) && now - c.issued < MAX_AGE_MS)
                return "board".equals(part) ? c.board : "piece".equals(part) ? c.piece : null;
        }
        return null;
    }

}
