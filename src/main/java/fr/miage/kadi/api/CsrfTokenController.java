package fr.miage.kadi.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/csrf")
public class CsrfTokenController {

    @GetMapping
    public ResponseEntity<CsrfTokenDto> token(CsrfToken csrfToken) {
        CsrfTokenDto response = new CsrfTokenDto(
                csrfToken.getToken(),
                csrfToken.getHeaderName());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
