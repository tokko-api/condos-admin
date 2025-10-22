package com.condos.user.domain;

import com.condos.user.model.UserRole;
import com.condos.user.model.UserStatus;
import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserProfileRepository repo;

    public DataSeeder(UserProfileRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        String id = "e0de4c67-c035-4d02-860d-b949da947eb1"; // pon el mismo _id del AuthAccount superadmin
        if (!repo.existsById(id)) {
            var profile = new UserProfile();
            profile.id = id;
            profile.email = "superadmin@system.local";
            profile.fullName = "System Super Admin";

            var ra = new UserProfile.OrgAssignment();
            ra.orgId = new ObjectId("000000000000000000000000");
            ra.role = UserRole.SUPERADMIN;
            ra.status = UserStatus.ACTIVE;
            profile.orgAssignments.add(ra);

            repo.save(profile);
            System.out.println("✅ Seeded SUPERADMIN profile in user-api");
        }
    }
}