package com.condos.auth.accounts;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document("auth_account")
public class AuthAccount {
    @Id public String id;
    @Indexed(unique = true) public String email;
    public String passwordHash;
    public Long accountVersion; // p.ej. inicia en 1L
    public Date createdAt;
    public Date updatedAt;
}