package sg.edu.iss.cats.service;

import java.nio.charset.StandardCharsets;

/** Quoted RFC4180 UTF-8 cells; apostrophe-prefix values that spreadsheet apps may treat as formulas. */
public final class CsvEncoder {
    private CsvEncoder() {}
    public static String cell(Object input) {
        String value = input == null ? "" : input.toString();
        int i = 0;
        while (i < value.length() && (Character.isWhitespace(value.charAt(i))
                || Character.isSpaceChar(value.charAt(i)) || value.charAt(i) == '\u200B'
                || value.charAt(i) == '\uFEFF')) i++;
        boolean control = !value.isEmpty() && (value.charAt(0) == '\t' || value.charAt(0) == '\r' || value.charAt(0) == '\n');
        if (control || (i < value.length() && "=+-@".indexOf(value.charAt(i)) >= 0)) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    public static void append(StringBuilder csv, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(cell(cells[i]));
        }
        csv.append("\r\n");
    }
    public static byte[] bytes(StringBuilder csv) { return csv.toString().getBytes(StandardCharsets.UTF_8); }
}
