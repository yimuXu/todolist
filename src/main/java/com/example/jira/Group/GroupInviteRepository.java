package com.example.jira.Group;

import com.example.jira.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupInviteRepository extends JpaRepository<GroupInvite,Integer> {
    List<GroupInvite> findByInviteeAndStatus(User invitee, InviteStatus status);
    List<GroupInvite> findByGroup(Group group);
    boolean existsByGroupAndInviteeAndStatus(Group group, User invitee,InviteStatus status);
}
