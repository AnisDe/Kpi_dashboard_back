package com.example.demo.service;

import com.example.demo.entity.Utilisateur;
import com.example.demo.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServices {
    @Autowired

    UserRepo userRepo;

    public Utilisateur addUser(Utilisateur user) {
        return userRepo.save(user);
    }
    public List<Utilisateur> getAllUsers() {
        return userRepo.findAll();

    }
    public Utilisateur getUserById(String userId) {
        return userRepo.findById(userId).orElse(null);

    }

}
