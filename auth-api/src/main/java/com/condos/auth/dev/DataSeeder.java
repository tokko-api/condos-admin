package com.condos.auth.dev;

import com.condos.auth.accounts.AuthAccount;
import com.condos.auth.accounts.AuthAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private final AuthAccountRepository repo;
    private final PasswordEncoder pe;

    public DataSeeder(AuthAccountRepository repo, PasswordEncoder pe) {
        this.repo = repo;
        this.pe = pe;
    }

    @Override
    public void run(String... args) {
        String email = "superadmin@system.local";
        if (repo.findByEmail(email).isEmpty()) {
            var acc = new AuthAccount();
            acc.id = UUID.randomUUID().toString();
            acc.email = email;
            acc.passwordHash = pe.encode("changeme");
            acc.accountVersion = 1L;
            acc.createdAt = new Date();
            acc.updatedAt = new Date();

            repo.save(acc);
            System.out.println("✅ Seeded AuthAccount for SUPERADMIN: " + email + " / changeme");
        }
    }
}