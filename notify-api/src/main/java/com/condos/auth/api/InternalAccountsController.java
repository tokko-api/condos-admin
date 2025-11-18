package com.condos.auth.api;

import com.condos.auth.service.AccountsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/condos/api/auth/internal")
@RequiredArgsConstructor
public class InternalAccountsController {

    @Value("${internal.secret}") private String internalSecret;
    private final AccountsService service;

    private void check(String header) {
        if (header == null || !header.equals(internalSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad internal secret");
        }
    }

    @PostMapping("/accounts")
    public Resp create(@RequestHeader("X-Internal-Secret") String secret,
                       @RequestBody CreateReq req) {
        check(secret);
        return service.create(req); // crea auth_account y devuelve {id, email}
    }

    @GetMapping("/accounts/by-email")
    public Resp byEmail(@RequestHeader("X-Internal-Secret") String secret,
                        @RequestParam String email) {
        check(secret);
        return service.byEmail(email);
    }

    // DTOs
    public record CreateReq(String email, String password) {}
    public record Resp(String id, String email) {}
}
