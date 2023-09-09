package com.example.demo.controller;

import com.example.demo.entity.Database;
import com.example.demo.entity.Utilisateur;
import com.example.demo.repository.DatabaseRepo;
import com.example.demo.repository.UserRepo;
import com.example.demo.service.DatabaseMetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/tables")
public class TableController {


    private final DatabaseMetadataService databaseMetadataService;
    private final DatabaseRepo dbRepo;
    private final UserRepo userRepository;

    public TableController(DatabaseMetadataService databaseMetadataService, DatabaseRepo dbRepo, UserRepo userRepository) {
        this.databaseMetadataService = databaseMetadataService;
        this.dbRepo = dbRepo;
        this.userRepository = userRepository;
    }


    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody Map<String, String> request) {
        String databaseType = request.get("selectedDatabaseType");
        String databaseName = request.get("databaseName");
        String url = request.get("url");
        String username = request.get("username");
        String password = request.get("password");

        try {
            databaseMetadataService.connect(url, username, password);

            JSONArray tables;
            if (isMongoDB(url)) {
                tables = databaseMetadataService.getMongoMetaData(url); // Assuming getMongoMetaData method is implemented
            } else {
                tables = databaseMetadataService.getSqlMetaData(url, username, password); // Assuming getSqlMetaData method is implemented
            }

/*
            databaseMetadataService.startRealTimeUpdates(url, username, password);
*/
            Map<String, Map<String, Map<String, List<String>>>> result;
            if (isMongoDB(url)) {
                result = databaseMetadataService.buildTableInfoMongo(url , tables); // Assuming buildTableInfoMongo method is implemented

            } else {
                result = databaseMetadataService.buildTableInfoSql(url, username, password, tables); // Assuming buildTableInfoSql method is implemented
            }
            Database db = new Database();
            db.setType(databaseType);
            db.setDatabaseName(databaseName);
            db.setUrl(url);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String connectedUsername = authentication.getName();
            Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);

            db.setUtilisateur(utilisateur);
            dbRepo.save(db);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            databaseMetadataService.stopRealTimeUpdates();
            String errorMessage = "Failed to connect to the database: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }
    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        System.out.println(query);
        try {
            List<Map<String, Object>> result = databaseMetadataService.executeQueriesAndReturnFirstResult(query);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            String errorMessage = "Failed to execute the SQL query: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }
    @PostMapping("/executeMongoQuery")
    public ResponseEntity<?> executeMongoQueries(@RequestBody Map<String, Object> requestBody) {

        try {
            String connectionString = (String) requestBody.get("connectionString");
            String queryJson = (String) requestBody.get("query");
            System.out.println(queryJson);
            if (connectionString == null || connectionString.isEmpty() || queryJson == null || queryJson.isEmpty()) {
                return ResponseEntity.badRequest().body("Empty or invalid connection string/query");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> queryMap = objectMapper.readValue(queryJson, Map.class);

            List<Map<String, Object>> resultsMap = databaseMetadataService.executeMongoQueries(connectionString, queryMap);

            if (resultsMap.isEmpty()) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(resultsMap);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error parsing query JSON: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing queries: " + e.getMessage());
        }
    }


    private boolean isMongoDB(String url) {
        return url.startsWith("mongodb://");
    }

}
