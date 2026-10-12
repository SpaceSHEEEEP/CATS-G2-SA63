package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.service.CsvEncoder;
class CsvEncoderTests {
    @Test void escapesQuotesAndBlocksSpreadsheetFormula() {
        StringBuilder csv = new StringBuilder();
        CsvEncoder.append(csv, "=HYPERLINK(\"evil\")", "Smith, Jr", null);
        assertThat(csv.toString()).isEqualTo("\"'=HYPERLINK(\"\"evil\"\")\",\"Smith, Jr\",\"\"\r\n");
    }
}
