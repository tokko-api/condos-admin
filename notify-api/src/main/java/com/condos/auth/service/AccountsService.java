package com.condos.auth.service;

import com.condos.auth.accounts.AuthAccount;
import com.condos.auth.api.InternalAccountsController.CreateReq;
import com.condos.auth.api.InternalAccountsController.Resp;
import com.condos.auth.accounts.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AccountsService {

    private final AuthAccountRepository repo;
    private final PasswordEncoder passwordEncoder;

    public Resp create(CreateReq req) {
        var acc = new AuthAccount();
        acc.id = java.util.UUID.randomUUID().toString();
        acc.email = req.email();
        acc.passwordHash = passwordEncoder.encode(req.password());
        acc.accountVersion = 1L;

        repo.save(acc);
        return new Resp(acc.id, acc.email);
    }

    public Resp byEmail(String email) {
        var acc = repo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "account not found"));
        return new Resp(acc.id, acc.email);
    }
}