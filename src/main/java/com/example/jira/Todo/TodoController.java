package com.example.jira.Todo;


import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todo")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }
    // ----------------- todo list ---------------------------
    // get all of todo list
    @GetMapping("/{username}")
    public List<TodoList> getList(){
        return todoService.getList();
    }
    @GetMapping("/{username}/{listid}")
    public TodoList getTodoList(@PathVariable int listid) {
        return todoService.getTodolist(listid);
    }

    @PostMapping("/{username}/{listName}")
    public String createtodolist(@RequestBody TodoListRequest req) {
        todoService.createList(req);
        return "create todo list successfully";
    }
    @PostMapping("/{username}/{listid}/update")
    public TodoList updateTodoList(@PathVariable int listid,@RequestBody TodoListRequest req) {
        return todoService.updateTodolist(listid,req);
    }
    @DeleteMapping("/{username}/{listid}/delete")
    public void deleteTodoList(@PathVariable int listid) {
        todoService.deleteList(listid);
    }
    //---------------todo item----------------------------------------
    @PostMapping("/{username}/{listid}/add")
    public String addTodoItem(@PathVariable int listid,@RequestBody TodoItemRequest item) {
        todoService.addTodoItem(listid,item);
        return "add item successfully";
    }
    @PostMapping("/{username}/{listid}/{itemid}/{status}")
    public String updateTodoItemStatus(@PathVariable int listid, @PathVariable int itemid, @PathVariable ItemStatus status) {

        todoService.markStatus(listid, itemid, status);
        return "update item successfully";
    }
    @PostMapping("/{username}/{listid}/{itemid}")
    public String updateTodoItem(@PathVariable int listid,@PathVariable int itemid,@RequestBody TodoItemRequest req) {

        todoService.updateTodoItem(listid,itemid,req);
        return "update item successfully";
    }
    @DeleteMapping("/{username}/{listid}/{itemid}")
    public String deleteTodoItem(@PathVariable int listid,@PathVariable int itemid) {
        todoService.deleteTodoItem(listid,itemid);
        return "delete item successfully";
    }



}
