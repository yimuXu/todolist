package com.example.jira.Group;

import com.example.jira.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository  extends JpaRepository<Group,Integer> {
    List<Group> findByOwner(User user);
    List<Group> findByMembersContaining(User user);
}
