package tech.iraelie.ledgevent.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.iraelie.ledgevent.security.JwtService;

@RestController
@RequestMapping("/auth/")
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;

    @PostMapping("token")
    public String issueToken(@RequestParam String username) {
        return jwtService.generateToken(username);
    }
}
