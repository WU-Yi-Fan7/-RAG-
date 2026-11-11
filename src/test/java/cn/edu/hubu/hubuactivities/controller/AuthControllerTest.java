package cn.edu.hubu.hubuactivities.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.junit.jupiter.api.Assertions.*;

class AuthControllerTest {
// TestController.java

    @RestController
    @RequestMapping("/test")
    public class TestController {

        @GetMapping("/ping")
        public String ping() {
            return "pong";
        }
    }
}