package com.example.jira.Group;
import jakarta.persistence.*;
import com.example.jira.User.User;

@Entity
@Table(name = "group_invites")
public class GroupInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int inviteId;
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User invitee;

    private InviteStatus status;
    public GroupInvite() {}
    public GroupInvite(Group group, User invitee) {
        this.group = group;
        this.invitee = invitee;
        status = InviteStatus.PENDING;
    }
    public int getInviteId() {return inviteId;}
    public Group getGroup() {return group;}
    public User getInvitee() {return invitee;}
    public InviteStatus getStatus() {return status;}
    public void setStatus(InviteStatus status) {this.status = status;}
}
