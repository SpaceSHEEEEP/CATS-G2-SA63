package sg.edu.iss.cats.security;

public final class ActionConfirmation {
    private ActionConfirmation() {}

    public static void require(String provided, String expected) {
        if (!expected.equals(provided)) {
            throw new IllegalArgumentException("Type " + expected + " to confirm this action.");
        }
    }
}
