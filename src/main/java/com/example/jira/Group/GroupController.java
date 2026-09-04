package com.example.jira.Group;

import com.example.jira.Todo.ItemStatus;
import com.example.jira.Todo.TodoItemRequest;
import com.example.jira.Todo.TodoList;
import com.example.jira.Todo.TodoListRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public Group createGroup(@RequestParam String name) {
        return groupService.createGroup(name);
    }
    @PostMapping("/update")
    public Group updateGroup(@RequestParam int groupId, @RequestParam String name) {
        return groupService.updateGroup(groupId, name);
    }

    @DeleteMapping("/{groupId}/delete")
    public void deleteGroup(@PathVariable int groupId) {
        groupService.deleteGroup(groupId);
    }

    @GetMapping("/mine")
    public List<Group> getMyGroups() {
        return groupService.getMyGroups();
    }


    @PostMapping("/{groupId}/invite")
    public String inviteUserToGroup(@PathVariable int groupId, @RequestParam String email) {
        return groupService.inviteUser(groupId, email);
    }

    @GetMapping("/invites/Sent")
    public List<GroupInvite> getSentInvites(){
        return groupService.getSentInvites();
    }

    @GetMapping("/invites/received")
    public List<GroupInvite> getReceivedInvites() {
        return groupService.getReceivedInvites();
    }

    @PostMapping("/invites/{inviteId}/respond")
    public String respondInvite(@PathVariable int inviteId, @RequestParam boolean accept) {
        return groupService.respondInvite(inviteId, accept);
    }

    @PostMapping("/{groupId}/lists/create")
    public TodoList createGroupList(@PathVariable int groupId, @RequestBody TodoListRequest list) {
        return groupService.createGroupTodoList(groupId, list);
    }
    @PostMapping("/{groupId}/lists/{listId}/update")
    public TodoList updateGroupList(@PathVariable int groupId, @PathVariable int listId, @RequestBody TodoListRequest list) {
        return groupService.updateGroupTodoList(groupId, listId, list);
    }
    @DeleteMapping("/{groupId}/lists/{listId}/delete")
    public void deleteGroupList(@PathVariable int groupId, @PathVariable int listId) {
        groupService.deleteGroupTodoList(groupId, listId);
    }
    @PostMapping("/{groupId}/lists/{listId}/items/create")
    public void createGroupItem(@PathVariable int groupId, @PathVariable int listId, @RequestBody TodoItemRequest item) {
        groupService.createGroupTodoItem(groupId, listId, item);
    }
    @PostMapping("/{groupId}/lists/{listId}/items/{itemId}/update")
    public void updateGroupItem(@PathVariable int groupId, @PathVariable int listId, @PathVariable int itemId, @RequestBody TodoItemRequest item) {
        groupService.updateGroupTodoItem(groupId, listId, itemId, item);
    }
    @DeleteMapping("/{groupId}/lists/{listId}/items/{itemId}/delete")
    public void deleteGroupItem(@PathVariable int groupId, @PathVariable int listId, @PathVariable int itemId) {
        groupService.deleteGroupTodoItem(groupId, listId, itemId);
    }
    // Used by drag-and-drop on the board, and by the status dropdown in the edit dialog.
    @PostMapping("/{groupId}/lists/{listId}/items/{itemId}/status/{status}")
    public String updateGroupItemStatus(@PathVariable int groupId, @PathVariable int listId,
                                        @PathVariable int itemId, @PathVariable ItemStatus status) {
        groupService.markGroupTodoItemStatus(groupId, listId, itemId, status);
        return "update status successfully";
    }

    @GetMapping("/{groupId}/lists")
    public List<TodoList> getGroupLists(@PathVariable int groupId) {
        return groupService.getGroupLists(groupId);
    }

    @DeleteMapping("/{groupId}/deletemember")
    public void deleteGroupMember(@PathVariable int groupId, @RequestParam String email) {
        groupService.deleteGroupMember(groupId, email);
    }
}
