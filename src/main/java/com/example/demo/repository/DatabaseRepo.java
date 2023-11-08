package com.example.demo.repository;

import com.example.demo.entity.Database;
import com.example.demo.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatabaseRepo extends JpaRepository<Database, Long> {
    Database findByDatabaseName(String databaseName);

    Database findByUrl(String url);

    List<Database> findAllByUrl(String dbUrl);

    Database findByUrlAndUtilisateurAndType(String url, Utilisateur utilisateur, String databaseType);

    List<Database> findByUtilisateurId(Object connectedUsername);
}
