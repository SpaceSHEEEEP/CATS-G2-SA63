package sg.edu.iss.cats.mobile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MobileEntryControllerTests {
    @Test void rootWithOrWithoutSlashForwardsToBuiltMobileApp() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new MobileEntryController()).build();
        mvc.perform(get("/mobile/app/"))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/mobile/app/index.html"));
        mvc.perform(get("/mobile/app"))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/mobile/app/index.html"));
    }
}
