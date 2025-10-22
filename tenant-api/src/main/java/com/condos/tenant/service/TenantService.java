package com.condos.tenant.service;

import com.condos.tenant.model.Tenant;
import com.condos.tenant.model.TenantStatus;
import com.condos.tenant.repository.TenantRepository;
import com.condos.tenant.web.dto.CreateTenantRequest;
import com.condos.tenant.web.dto.UpdateTenantRequest;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final TenantRepository repo;

    public TenantService(TenantRepository repo) { this.repo = repo; }

    public Tenant create(CreateTenantRequest req) {
        var now = Instant.now();
        var t = new Tenant();
        t.name = (req.name().trim());
        t.slug = (req.slug().trim());
        t.email = (req.email());
        t.phone = (req.phone());
        t.address = (req.address());
        t.settings = (req.settings());
        t.status = TenantStatus.ACTIVE;
        t.createdAt = (now);
        t.updatedAt = (now);
        t.active = (true);
        try { return repo.save(t); }
        catch (DuplicateKeyException e) { throw e; }
    }

    public Tenant getActiveById(String id) {
        var oid = new ObjectId(id);                    // valida formato
        var t = repo.findById(oid).orElseThrow(() -> new NoSuchElementException("tenant not found"));
        if (t.status == TenantStatus.ARCHIVED) throw new NoSuchElementException("tenant archived");
        return t;
    }

    public Page<Tenant> list(String q, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        var pageAll = repo.findAll(pageable);

        if (q == null || q.isBlank()) return pageAll;

        var filtered = pageAll.getContent().stream()
                .filter(t -> contains(t.name, q) || contains(t.slug, q) || contains(t.email, q))
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public Tenant getBySlugActive(String slug) {
        return repo.findBySlug(slug)
                .filter(t -> t.status != TenantStatus.ARCHIVED)
                .orElseThrow(() -> new NoSuchElementException("tenant not found"));
    }

    public Page<Tenant> listActive(String q, int page, int size, String sortBy, Sort.Direction dir) {
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        var all = repo.findAll(pageable)
                .map(t -> t) // page
                .map(t -> t); // noop (ilustrativo)
        var filtered = all.getContent().stream()
                .filter(t -> t.status != TenantStatus.ARCHIVED)
                .filter(t -> q == null || contains(t.name, q) || contains(t.slug, q) || contains(t.email, q))
                .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q.toLowerCase());
    }

    public Tenant update(String id, UpdateTenantRequest req) {
        var t = getActiveById(id);
        if (StringUtils.hasText(req.name())) t.name = (req.name().trim());
        if (StringUtils.hasText(req.slug())) t.slug = (req.slug().trim()); // respeta índice único parcial
        if (req.email() != null) t.email = (req.email());
        if (req.phone() != null) t.phone = (req.phone());
        if (req.address() != null) t.address = (req.address());
        if (req.settings() != null) t.settings = (req.settings());
        t.updatedAt = (Instant.now());
        return repo.save(t);
    }

    /** Soft delete */
    public void archive(String id, String userId, String reason) {
        var oid = new ObjectId(id);
        var t = repo.findById(oid).orElseThrow(() -> new NoSuchElementException("tenant not found"));
        if (t.status == TenantStatus.ARCHIVED) return;
        t.status = (TenantStatus.ARCHIVED);
        t.active = (false);
        t.archivedAt = (Instant.now());
        t.archivedBy = (userId);
        t.archivedReason = (reason);
        t.updatedAt = (Instant.now());
        repo.save(t);
    }

    /** Reactivar */
    public void activate(String id) {
        var oid = new ObjectId(id);
        var t = repo.findById(oid).orElseThrow(() -> new NoSuchElementException("tenant not found"));
        t.status = (TenantStatus.ACTIVE);
        t.active = (true);
        t.updatedAt = (Instant.now());
        repo.save(t);
    }

    /** Suspender (bloqueo temporal) */
    public void suspend(String id) {
        var t = getActiveById(id);
        t.status = (TenantStatus.SUSPENDED);
        t.active = (true);
        t.updatedAt = (Instant.now());
        repo.save(t);
    }


    public List<Tenant> getByIds(List<String> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids required");
        }

        // Soporta "?ids=a,b,c" y "?ids=a&ids=b&ids=c"
        List<String> flat = rawIds.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Valida y convierte a ObjectId
        List<ObjectId> oids = new ArrayList<>(flat.size());
        for (String s : flat) {
            if (!ObjectId.isValid(s)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid tenant id");
            }
            oids.add(new ObjectId(s));
        }

        // Busca en repo
        List<Tenant> found = repo.findAllById(oids);

        // (Opcional) mantener el orden solicitado
        Map<String, Tenant> byId = found.stream()
                .collect(Collectors.toMap(t -> t.id.toHexString(), Function.identity()));

        return flat.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}