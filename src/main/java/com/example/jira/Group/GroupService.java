package com.example.jira.Group;

import com.example.jira.Todo.*;
import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import com.example.jira.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final GroupInviteRepository groupInviteRepository;
    private final UserRepository userRepository;
    private final TodoListRepository todoListRepository;
    private final TodoService todoService;
    private final TodoItemRepository todoItemRepository;

    public GroupService(GroupRepository groupRepository, UserService userService, GroupInviteRepository groupInviteRepository, UserRepository userRepository, TodoListRepository todoListRepository, TodoService todoService, TodoItemRepository todoItemRepository) {
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.groupInviteRepository = groupInviteRepository;
        this.userRepository = userRepository;
        this.todoListRepository = todoListRepository;
        this.todoService = todoService;
        this.todoItemRepository = todoItemRepository;
    }
    // --------------------------GROUP-----------------------------
    public Group createGroup(String name) {
        User user = userService.getCurrentUser();
        Group group = new Group(name, user);
        group.getMembers().add(user);
        return groupRepository.save(group);
    }

    public List<Group> getMyGroups() {
        User user = userService.getCurrentUser();
        List<Group> owned = groupRepository.findByOwner(user);
        List<Group> member = groupRepository.findByMembersContaining(user);
        // Merge without duplicates. Group has no equals(), so compare on the id rather than
        // relying on both queries handing back the same instance.
        List<Group> all = new ArrayList<>(owned);
        Set<Integer> seen = owned.stream().map(Group::getGroupId).collect(Collectors.toSet());
        member.stream().filter(g -> seen.add(g.getGroupId())).forEach(all::add);
        return all;
    }

    public String inviteUser(int groupId, String username) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!group.getOwner().getUsername().equals(currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this group");
        }
        User invitee = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (groupInviteRepository.existsByGroupAndInviteeAndStatus(group, invitee, InviteStatus.PENDING)) {
            return "Please do not invite repeatedly.";
        }
        groupInviteRepository.save(new GroupInvite(group, invitee));
        return "Invite sent to " + username;
    }

    //get my invites
    public List<GroupInvite> getSentInvites(){
        User user = userService.getCurrentUser();
        List<Group> groups = groupRepository.findByOwner(user);
        List<GroupInvite> invites = new ArrayList<>();
        for (Group group : groups) {
            List<GroupInvite> inviteList = groupInviteRepository.findByGroup(group);
            invites.addAll(inviteList);
        }
        return invites;
    }

    public List<GroupInvite> getReceivedInvites() {
        User user = userService.getCurrentUser();
        return groupInviteRepository.findByInviteeAndStatus(user, InviteStatus.PENDING);
    }

    //accept decline
    public String respondInvite(int inviteId, boolean accept) {
        User currentUser = userService.getCurrentUser();
        GroupInvite invite = groupInviteRepository.findById(inviteId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!invite.getInvitee().getUsername().equals(currentUser.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        }
        if(accept){
            invite.setStatus(InviteStatus.ACCEPTED);
            List<User> members = invite.getGroup().getMembers();
            if (members.stream().noneMatch(m -> m.getUsername().equals(currentUser.getUsername()))) {
                members.add(currentUser);
            }
            groupRepository.save(invite.getGroup());
        }
        else{
            invite.setStatus(InviteStatus.DECLINED);
        }
        groupInviteRepository.save(invite);
        return accept?"accepted":"declined";
    }

    public Group updateGroup(int groupId,String name){
        // Used to return null for a non-owner, which the client saw as a 200 with an empty body.
        if(!isOwner(groupId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this group");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        group.setGroupName(name);
        return groupRepository.save(group);
    }

    public void deleteGroup(int groupId){
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (isOwner(groupId)){
            groupRepository.delete(group);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,""+userService.getCurrentUser().getUsername()+" is not the owner of this group");
        }
    }

    public void deleteGroupMember(int groupId,String username){
        // Used to silently do nothing for a non-owner, which looked like success in the UI.
        if(!isOwner(groupId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this group");
        }
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        group.getMembers().removeIf(member -> member.getUsername().equals(username));
        groupRepository.save(group);
    }

    //get all lists in a group
    public List<TodoList> getGroupLists(int groupId) {
        Group group = checkUserPermission(groupId);
        return group.getTodoLists();
    }
    // ----------- GROUP TODOLIST--------------------------------------
    public TodoList createGroupTodoList(int groupId, TodoListRequest req) {

        Group group = checkUserPermission(groupId);
        TodoList newlist = todoService.createList(req);
        newlist.setGroup(group);
        return todoListRepository.save(newlist);
    }

    public TodoList updateGroupTodoList(int groupId,int listId, TodoListRequest req){
        checkUserPermission(groupId);
        return todoService.updateTodolist(listId,req);
    }

    public void deleteGroupTodoList(int groupId,int listId){
        checkUserPermission(groupId);
        todoService.deleteList(listId);
    }

    public void createGroupTodoItem(int groupId,int listId, TodoItemRequest req){
        User currentUser = userService.getCurrentUser();
        TodoList list = groupList(groupId, listId);

        TodoItem item = new TodoItem();
        if (req.getname() != null) item.setItemName(req.getname());
        if (req.getdescription() != null) item.setItemDescription(req.getdescription());
        if (req.getdate() != null) item.setDueDate(req.getdate());
        item.setTodoList(list);
        item.setAddedBy(currentUser);
        todoItemRepository.save(item);
    }

    public void updateGroupTodoItem(int groupId,int listId,int itemId, TodoItemRequest req){
        todoService.updateItemIn(groupList(groupId, listId), itemId, req);
    }

    public void deleteGroupTodoItem(int groupId,int listId,int itemId){
        todoService.deleteItemIn(groupList(groupId, listId), itemId);
    }

    /** Moves a task between the To Do / In Progress / Done columns of a shared list. */
    public void markGroupTodoItemStatus(int groupId, int listId, int itemId, ItemStatus status) {
        TodoList list = groupList(groupId, listId);
        TodoItem item = todoItemRepository.findByIdAndTodoList(itemId, list)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        item.setItemStatus(status);
        todoItemRepository.save(item);
    }

    /**
     * The shared list, checked twice: the caller must be in the group, and the list must belong
     * to that group. Going through TodoService instead would check that the caller *owns* the
     * list, which on a shared board only whoever created it does — every other member would be
     * refused when adding, editing, moving or deleting a card. Confirming the list really is
     * this group's is what keeps the looser check safe: a member cannot reach another group's
     * list, or somebody's private list, by passing its id.
     */
    private TodoList groupList(int groupId, int listId) {
        Group group = checkUserPermission(groupId);
        TodoList list = todoListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
        if (list.getGroup() == null || list.getGroup().getGroupId() != group.getGroupId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That list does not belong to this group");
        }
        return list;
    }

    private boolean isOwner(int groupId) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return group.getOwner().getUsername().equals(currentUser.getUsername());
    }

    private Group checkUserPermission(int groupId) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findById(groupId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean isMember = group.getMembers().stream().anyMatch(u -> u.getUsername().equals(currentUser.getUsername()));
        if(!isMember){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,""+currentUser.getUsername()+" is not a member of this group");
        }
        return group;
    }
}
