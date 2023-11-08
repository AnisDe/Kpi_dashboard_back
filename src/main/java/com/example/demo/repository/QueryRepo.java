package com.example.demo.repository;

import com.example.demo.entity.Database;
import com.example.demo.entity.Querys;
import com.example.demo.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QueryRepo extends JpaRepository<Querys, Long> {

    List<Querys> findByDatabaseId(Long databaseId);
    @Query("SELECT q FROM Querys q WHERE q.name = :queryName " +
            "AND q.conditions = :queryConditions " +
            "AND q.user = :utilisateur " +
            "AND q.database = :selectedDatabase")
    List<Querys> findDuplicateQueries(@Param("queryName") String queryName,
                                      @Param("queryConditions") String queryConditions,
                                      @Param("utilisateur") Utilisateur utilisateur,
                                      @Param("selectedDatabase") Database selectedDatabase);

}


