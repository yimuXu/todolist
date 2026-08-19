package com.example.jira.Todo;

import com.example.jira.User.User;
import com.example.jira.User.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TodoService {

    private final UserService userService;
    private final TodoItemRepository todoItemRepository;
    private final TodoListRepository todoListRepository;
    public TodoService(UserService userService, TodoItemRepository todoItemRepository, TodoListRepository todoListRepository) {
        this.userService = userService;
        this.todoItemRepository = todoItemRepository;
        this.todoListRepository = todoListRepository;
    }

    //--------------------------TODO LIST-----------------------------

    // get lists
    public List<TodoList> getList(){
        User user =  userService.getCurrentUser();
        return  todoListRepository.findByUser(user);
    }
    // helper find todolist
    public TodoList getTodolist(int listId){
        User user =  userService.getCurrentUser();
        return todoListRepository.findByTodoListIdAndUser(listId, user).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
    }


    // create new todo list and add it to user's list
    public TodoList createList(TodoListRequest req) {
        User user = userService.getCurrentUser();
        String name = req == null ? null : req.getTodoListname();
        // Returning null here used to leave callers (e.g. group list creation) with an NPE.
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List name is required");
        }
        TodoList todoList = new TodoList(name.trim());
        todoList.setUser(user);
        return todoListRepository.save(todoList);
    }
    //update list
    public TodoList updateTodolist(int listId, TodoListRequest req){
        TodoList lst = getTodolist(listId);
        lst.setTodoListname(req.getTodoListname());
        return todoListRepository.save(lst);
    }
    // delete list, together with every task inside it
    // Transactional so the list stays managed between the lookup and the delete: the cascade
    // that clears the tasks needs a live session, otherwise the detached collection is merged
    // back first and the delete can still trip the todoitem foreign key.
    @Transactional
    public void deleteList(int listId){
        TodoList lst = getTodolist(listId);
        todoListRepository.delete(lst);
    }


    //---------------------------TODO ITEM-------------------------------



    //  get current item
    public TodoItem getTodoItem(int listId, int itemId){
        return getItemIn(getTodolist(listId), itemId);
    }

    /**
     * Item lookup for a list the caller has already been cleared to touch. Group code resolves
     * the list through the group instead of through list ownership, so it needs this entry point
     * rather than the listId one.
     */
    public TodoItem getItemIn(TodoList list, int itemId){
        return todoItemRepository.findByIdAndTodoList(itemId, list)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
    // add new item to a todolist
    public void addTodoItem(int listId, TodoItemRequest req){
        User user =  userService.getCurrentUser();
        if(req == null){
            return;
        }
        TodoItem newitem = new TodoItem();
        if (req.getname() != null) {newitem.setItemName(req.getname());}
        if (req.getdescription() != null){newitem.setItemDescription(req.getdescription());}
        if(req.getdate()!=null){newitem.setDueDate(req.getdate());}
        newitem.setAddedBy(user);
        TodoList lst = getTodolist(listId);
        newitem.setTodoList(lst);
        todoItemRepository.save(newitem);
    }
    // update
    public TodoItem updateTodoItem(int listId,int itemId,TodoItemRequest req){
        return updateItemIn(getTodolist(listId), itemId, req);
    }

    public TodoItem updateItemIn(TodoList list,int itemId,TodoItemRequest req){
        TodoItem item = getItemIn(list, itemId);
        if (req.getname() != null) {item.setItemName(req.getname());}
        if (req.getdescription() != null){item.setItemDescription(req.getdescription());}
        if(req.getdate()!=null){item.setDueDate(req.getdate());}
        return todoItemRepository.save(item);
    }
    // delete
    @Transactional
    public void deleteTodoItem(int listId,int itemId){
        deleteItemIn(getTodolist(listId), itemId);
    }

    /**
     * Deleting the row is not enough on its own. TodoList maps its tasks EAGER with
     * CascadeType.ALL, so the list loaded a moment ago still holds the task in its collection;
     * when Hibernate flushes it cascades a persist back over that collection and re-attaches the
     * task it was just told to remove. The DELETE returns 200, the task comes straight back on
     * the next load, and it looks like the delete silently did nothing. Taking the task out of
     * the parent's collection first is what makes the removal survive the flush — with
     * orphanRemoval on that alone would delete it, and the explicit delete plus flush keeps the
     * failure loud if anything else still points at the row.
     */
    @Transactional
    public void deleteItemIn(TodoList list,int itemId){
        TodoItem item = getItemIn(list, itemId);
        List<TodoItem> siblings = list.getTodoItem();
        if (siblings != null) {
            siblings.removeIf(sibling -> sibling.getId() == itemId);
        }
        item.setTodoList(null);
        todoItemRepository.delete(item);
        todoItemRepository.flush();
    }



    //change item status  including todo  on progress done
    public String markStatus(int listId, int itemId, ItemStatus status){
        TodoItem todoItem = getTodoItem(listId,itemId);
        todoItem.setItemStatus(status);
        todoItemRepository.save(todoItem);
        return "status changed successfully";
    }
}
