package sg.edu.iss.cats.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MfaCaptchaTemplateTests {
    @Test void mfaFormsNeverDemandACaptchaPuzzle() throws Exception {
        for (String template : new String[] {"mfa-enroll.html", "mfa-challenge.html"}) {
            try (var stream = getClass().getResourceAsStream("/templates/" + template)) {
                assertThat(stream).as(template).isNotNull();
                String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(html).doesNotContain("fragments/captcha", "captcha.js", "captcha-dialog.js");
            }
        }
    }
}
