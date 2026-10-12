package sg.edu.iss.cats;

import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.security.ActionConfirmation;

import static org.assertj.core.api.Assertions.*;

class ActionConfirmationTests {
    @Test void requiredPhrasesAreExactAndCaseSensitive() {
        assertThatCode(() -> ActionConfirmation.require("DELETE", "DELETE")).doesNotThrowAnyException();
        assertThatCode(() -> ActionConfirmation.require("RESET MFA", "RESET MFA")).doesNotThrowAnyException();
        assertThatThrownBy(() -> ActionConfirmation.require("delete", "DELETE"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("DELETE");
        assertThatThrownBy(() -> ActionConfirmation.require("DELETE ", "DELETE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActionConfirmation.require(null, "REVOKE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
