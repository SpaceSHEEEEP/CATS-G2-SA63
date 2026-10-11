package sg.edu.iss.cats.service;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

/**
 * Generates a stable, spreadsheet-safe export without exposing passwords or other account details.
 */
@Service
public class UserCsvService {
    private final UserRepository users;

    public UserCsvService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] generateCsv() {
        StringBuilder csv = new StringBuilder("userId,name,isActive,isAdmin,email,managerName\r\n");

        for (User user : users.findAllByOrderByUserIdAsc()) {
            csv.append(user.getUserId()).append(',')
                    .append(csvCell(user.getName())).append(',')
                    .append(user.isEnabled()).append(',')
                    .append(user.getRole() == Role.ROLE_ADMIN).append(',')
                    .append(csvCell(user.getEmail())).append(',')
                    .append(csvCell(user.getManager() == null ? null : user.getManager().getName()))
                    .append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * RFC 4180 quoting plus a leading apostrophe for spreadsheet formula-like input.
     * Quoting alone does not prevent formulas from executing in Excel/Sheets.
     */
    private static String csvCell(String value) {
        if (value == null) {
            value = "";
        }

        int firstVisible = 0;
        while (firstVisible < value.length()) {
            char ch = value.charAt(firstVisible);
            if (!Character.isWhitespace(ch) && !Character.isSpaceChar(ch)
                    && ch != '\u200B' && ch != '\uFEFF') {
                break;
            }
            firstVisible++;
        }

        boolean startsWithControl = !value.isEmpty()
                && (value.charAt(0) == '\t' || value.charAt(0) == '\r' || value.charAt(0) == '\n');
        boolean looksLikeFormula = firstVisible < value.length()
                && "=+-@".indexOf(value.charAt(firstVisible)) >= 0;
        if (startsWithControl || looksLikeFormula) {
            value = "'" + value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
