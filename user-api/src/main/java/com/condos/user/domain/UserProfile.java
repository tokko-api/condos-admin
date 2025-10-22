package com.condos.user.domain;

import com.condos.user.model.UserRole;
import com.condos.user.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    public String id;   // 🔹 debe ser String, para coincidir con el id de AuthAccount (UUID string)

    public String email;
    public String fullName;

    @Builder.Default
    public List<OrgAssignment> orgAssignments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrgAssignment {
        public ObjectId orgId;     // aquí sí conviene mantenerlo como ObjectId
        public UserRole role;      // SUPERADMIN, ADMINISTRADOR, SUPERVISOR, OPERATIVO
        public UserStatus status;  // ACTIVE, INACTIVE
    }
}