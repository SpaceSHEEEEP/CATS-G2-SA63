package sg.edu.iss.cats.controller;

import java.time.Year;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.TrainingLedgerService;

@Controller
public class TrainingLedgerController {
    private final TrainingLedgerService ledgers;
    private final UserRepository users;
    public TrainingLedgerController(TrainingLedgerService ledgers, UserRepository users) {
        this.ledgers = ledgers; this.users = users;
    }

    @GetMapping("/staff/ledger")
    public String personal(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? Year.now(ZoneId.of("Asia/Singapore")).getValue() : year;
        User employee = users.findById(principal.getUserId()).orElseThrow();
        showLedger(model, employee, y, false);
        return "ledger";
    }

    @GetMapping("/manager/ledger")
    public String subordinate(@AuthenticationPrincipal User principal,
            @RequestParam Integer employeeId, @RequestParam(required = false) Integer year,
            Model model) {
        User employee = users.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (principal.getRole() != Role.ROLE_MANAGER
                || !users.existsByUserIdAndManager_UserId(employeeId, principal.getUserId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your direct subordinate");
        int y = year == null ? Year.now(ZoneId.of("Asia/Singapore")).getValue() : year;
        showLedger(model, employee, y, true);
        return "ledger";
    }

    private void showLedger(Model model, User employee, int year, boolean managerView) {
        if (year < 2000 || year > 9999) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        model.addAttribute("employee", employee);
        model.addAttribute("managerView", managerView);
        model.addAttribute("ledger", ledgers.forEmployee(employee, year));
    }
}
