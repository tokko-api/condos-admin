package com.condos.board.security;

import com.condos.board.repository.AmenityRepository;
import com.condos.board.repository.AnnouncementRepository;
import com.condos.board.repository.BoardRepository;
import com.condos.board.repository.ReservationRepository;
import com.condos.board.repository.TaskRepository;
import com.condos.board.repository.UnitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("jwtAuth")
public class JwtAuth {

    private final BoardRepository boards;
    private final TaskRepository tasks;
    private final UnitRepository units;
    private final AmenityRepository amenities;
    private final ReservationRepository reservations;
    private final AnnouncementRepository announcements;

    public JwtAuth(BoardRepository boards, TaskRepository tasks, UnitRepository units,
                    AmenityRepository amenities, ReservationRepository reservations,
                    AnnouncementRepository announcements) {
        this.boards = boards;
        this.tasks = tasks;
        this.units = units;
        this.amenities = amenities;
        this.reservations = reservations;
        this.announcements = announcements;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claims(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jat) {
            return jat.getTokenAttributes(); // ← estándar con resource server
        }
        Object details = auth != null ? auth.getDetails() : null;
        if (details instanceof Map<?,?> map) {
            // convertir a Map<String,Object> seguro
            Map<String,Object> out = new HashMap<>();
            map.forEach((k,v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Collections.emptyMap();
    }

    /** ¿Tiene rol SUPERADMIN global? */
    public boolean isSuperadmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;

        var c = claims(auth);

        Object roles = c.get("roles");
        if (roles instanceof Collection<?> col) {
            for (Object r : col) {
                if ("SUPERADMIN".equals(String.valueOf(r))) return true;
            }
        }
        // fallback: algunos tokens solo llevan roles por org
        Object orgs = c.get("orgs");
        if (orgs instanceof Collection<?> col) {
            for (Object o : col) {
                if (o instanceof Map<?,?> m) {
                    String role = String.valueOf(m.get("role"));
                    if ("SUPERADMIN".equals(role)) return true;
                }
            }
        }
        return false;
    }

    /** ¿Tiene alguno de los roles requeridos dentro de ese orgId? */
    public boolean hasRoleInOrg(Authentication auth, String orgId, Collection<String> rolesReq) {
        if (auth == null || !auth.isAuthenticated()) return false;

        var c = claims(auth);
        Object orgs = c.get("orgs");
        if (!(orgs instanceof Collection<?> col)) return false;

        for (Object o : col) {
            if (o instanceof Map<?,?> m) {
                String oid  = String.valueOf(m.get("orgId"));
                String role = String.valueOf(m.get("role"));
                if (Objects.equals(orgId, oid) && rolesReq.contains(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este board? */
    public boolean hasAccessToBoard(Authentication auth, String boardId, Collection<String> rolesReq) {
        if (boardId == null) return false;
        var board = boards.findById(boardId).orElse(null);
        if (board == null) return false;
        return hasRoleInOrg(auth, board.orgId, rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño del board al que pertenece esta task? */
    public boolean hasAccessToTask(Authentication auth, String taskId, Collection<String> rolesReq) {
        if (taskId == null) return false;
        var task = tasks.findById(taskId).orElse(null);
        if (task == null) return false;
        return hasRoleInOrg(auth, task.getOrgId(), rolesReq);
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de la unidad? */
    public boolean hasAccessToUnit(Authentication auth, String unitId, Collection<String> rolesReq) {
        if (unitId == null) return false;
        var unit = units.findById(unitId).orElse(null);
        if (unit == null) return false;
        return hasRoleInOrg(auth, unit.getOrgId(), rolesReq);
    }

    /** Resident (condomino) check: is this the unit's assigned resident? */
    public boolean isResidentOfUnit(Authentication auth, String unitId) {
        if (unitId == null || auth == null || !auth.isAuthenticated()) return false;
        var unit = units.findById(unitId).orElse(null);
        if (unit == null || unit.getResidentUserId() == null) return false;
        return unit.getResidentUserId().equals(auth.getName());
    }

    /** ¿Es residente (condomino) de alguna unidad de esta colonia? Le permite reportar incidencias ahí. */
    public boolean isResidentOfBoard(Authentication auth, String boardId) {
        if (boardId == null || auth == null || !auth.isAuthenticated()) return false;
        return units.findByResidentUserId(auth.getName()).stream()
                .anyMatch(u -> boardId.equals(u.getBoardId()));
    }

    /** ¿Es el operativo asignado a esta tarea/incidencia? Le permite cambiar su propio status. */
    public boolean isAssignedToTask(Authentication auth, String taskId) {
        if (taskId == null || auth == null || !auth.isAuthenticated()) return false;
        var task = tasks.findById(taskId).orElse(null);
        if (task == null || task.getAssigneeId() == null) return false;
        return task.getAssigneeId().equals(auth.getName());
    }

    /** ¿Es quien reportó esta tarea/incidencia (condómino u operativo)? Le permite adjuntar evidencia. */
    public boolean isReporterOfTask(Authentication auth, String taskId) {
        if (taskId == null || auth == null || !auth.isAuthenticated()) return false;
        var task = tasks.findById(taskId).orElse(null);
        if (task == null || task.getReportedBy() == null) return false;
        return task.getReportedBy().equals(auth.getName());
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de esta amenidad? */
    public boolean hasAccessToAmenity(Authentication auth, String amenityId, Collection<String> rolesReq) {
        if (amenityId == null) return false;
        var amenity = amenities.findById(amenityId).orElse(null);
        if (amenity == null) return false;
        return hasRoleInOrg(auth, amenity.getOrgId(), rolesReq);
    }

    /**
     * ¿Es residente de la colonia dueña de esta amenidad? A diferencia de
     * hasAccessToAmenity con rol CONDOMINO (que solo mira el org y dejaría
     * ver amenidades de OTRA colonia de la misma empresa), esto valida la
     * colonia (boardId) exacta.
     */
    public boolean isResidentOfAmenityBoard(Authentication auth, String amenityId) {
        if (amenityId == null) return false;
        var amenity = amenities.findById(amenityId).orElse(null);
        if (amenity == null) return false;
        return isResidentOfBoard(auth, amenity.getBoardId());
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de esta reservación? */
    public boolean hasAccessToReservation(Authentication auth, String reservationId, Collection<String> rolesReq) {
        if (reservationId == null) return false;
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null) return false;
        return hasRoleInOrg(auth, reservation.getOrgId(), rolesReq);
    }

    /** ¿Es quien hizo esta reservación? Le permite cancelarla. */
    public boolean isRequesterOfReservation(Authentication auth, String reservationId) {
        if (reservationId == null || auth == null || !auth.isAuthenticated()) return false;
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null || reservation.getRequestedBy() == null) return false;
        return reservation.getRequestedBy().equals(auth.getName());
    }

    /** ¿Tiene alguno de los roles requeridos en el org dueño de este comunicado? */
    public boolean hasAccessToAnnouncement(Authentication auth, String announcementId, Collection<String> rolesReq) {
        if (announcementId == null) return false;
        var announcement = announcements.findById(announcementId).orElse(null);
        if (announcement == null) return false;
        return hasRoleInOrg(auth, announcement.getOrgId(), rolesReq);
    }
}