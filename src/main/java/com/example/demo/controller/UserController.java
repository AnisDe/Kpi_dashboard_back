package com.example.demo.controller;
import com.example.demo.entity.Utilisateur;
import com.example.demo.service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController
{
    @Autowired
    UserServices userService;

    @PostMapping("/add")
    public ResponseEntity<Utilisateur> createProject(@RequestBody Utilisateur user) {
        Utilisateur  createdProject = userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }
    @GetMapping("/userById/{userId}")
    public Utilisateur getUserById(@PathVariable String userId ) {
        return userService.getUserById(userId);
    }
    @GetMapping("/allusers")
    public List<Utilisateur> getAllUsers() {
        return userService.getAllUsers();
    }

}
