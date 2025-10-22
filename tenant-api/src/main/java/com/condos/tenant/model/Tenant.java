package com.condos.tenant.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("tenants")
@CompoundIndex(name = "uq_active_slug",
        def = "{ 'slug': 1 }", unique = true)
public class Tenant {
    @Id
    public ObjectId id;

    @Indexed(unique = true) // evita nombres duplicados
    public String name;

    @Indexed(unique = true) // slug único para URLs/subdominios
    public String slug;

    public String email;       // correo de contacto
    public String phone;       // teléfono de contacto
    public String address;     // dirección opcional
    public Map<String, Object> settings; // config flexible por tenant

    public TenantStatus status;     // ACTIVE, SUSPENDED, ARCHIVED
    public boolean active;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant archivedAt;
    public String archivedBy;
    public String archivedReason;

    // getters/setters, equals/hashCode/toString…
}