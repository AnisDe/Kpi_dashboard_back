package com.example.demo.controller;

import com.example.demo.entity.Database;
import com.example.demo.entity.Querys;
import com.example.demo.entity.Utilisateur;
import com.example.demo.repository.DatabaseRepo;
import com.example.demo.repository.QueryRepo;
import com.example.demo.repository.UserRepo;
import com.example.demo.service.DatabaseMetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/tables")
public class TableController {


    private final DatabaseMetadataService databaseMetadataService;
    private final DatabaseRepo dbRepo;
    private final UserRepo userRepository;
    private final QueryRepo queryRepo;
    private Set<String> uniqueQueries = new HashSet<>();
    private Set<String> executedSupersetQueries = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(TableController.class);



    public TableController(DatabaseMetadataService databaseMetadataService, DatabaseRepo dbRepo, UserRepo userRepository, QueryRepo queryRepo) {
        this.databaseMetadataService = databaseMetadataService;
        this.dbRepo = dbRepo;
        this.queryRepo = queryRepo;
        this.userRepository = userRepository;
    }


    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody Map<String, String> request) {
        String databaseType = request.get("selectedDatabaseType");
        String databaseName = request.get("databaseName");
        String url = request.get("url");
        String username = request.get("username");
        String password = request.get("password");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String connectedUsername = authentication.getName();
        Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);

        // Check if a database with the same URL, user ID, and type already exists
        Database existingDatabase = dbRepo.findByUrlAndUtilisateurAndType(url, utilisateur, databaseType);

        if (existingDatabase != null) {
            // A database with the same criteria already exists, you can choose to return some information or perform additional actions if needed
            String message = "Database with the same URL, user ID, and type already exists.";
            System.out.println(message);
        }
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RefreshableKeycloakSecurityContext securityContext = (RefreshableKeycloakSecurityContext) keycloakPrincipal.getKeycloakSecurityContext();
        String accessToken = securityContext.getTokenString();
        System.out.println("Access Token: " + accessToken);

        try {
            // Regardless of whether the database exists or not, proceed with the connection
            databaseMetadataService.connect(url, username, password);

            JSONArray tables;
            if (isMongoDB(url)) {
                tables = databaseMetadataService.getMongoMetaData(url); // Assuming getMongoMetaData method is implemented
            } else {
                tables = databaseMetadataService.getSqlMetaData(url, username, password); // Assuming getSqlMetaData method is implemented
            }

            Map<String, Map<String, Map<String, List<String>>>> result;
            if (isMongoDB(url)) {
                result = databaseMetadataService.buildTableInfoMongo(url, tables); // Assuming buildTableInfoMongo method is implemented
            } else {
                result = databaseMetadataService.buildTableInfoSql(url, username, password, tables); // Assuming buildTableInfoSql method is implemented
            }

            // Create and save the database only if it doesn't already exist
            if (existingDatabase == null) {
                Database db = new Database();
                db.setType(databaseType);
                db.setDatabaseName(databaseName);
                db.setUrl(url);
                db.setUtilisateur(utilisateur);
                dbRepo.save(db);
            }

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            databaseMetadataService.stopRealTimeUpdates();
            String errorMessage = "Failed to connect to the database: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }





    @GetMapping("/user-databases")
    public ResponseEntity<?> getUserDatabases() {
        try {
            // Get the authenticated user's username
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean test = authentication.isAuthenticated();
            System.out.println("Authentication: " + authentication);
            Object connectedUsername = authentication.getName();
            System.out.println(connectedUsername);
            List<Database> userDatabases = dbRepo.findByUtilisateurId(connectedUsername);

            if (userDatabases.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(userDatabases);
        } catch (Exception e) {
            // Log the exception here
            e.printStackTrace();
            String errorMessage = "Failed to fetch databases for the current user: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

   /* @PostMapping("/query")

    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            String queryBuilders = request.get("queryBuilders");
            String queryName = request.get("queryName");
            if (query == null || query.isEmpty() || queryBuilders == null || queryBuilders.isEmpty() || queryName == null || queryName.isEmpty()) {
                return ResponseEntity.badRequest().body("Missing or empty request parameters.");
            }

            uniqueQueries.add(query);

            String dbUrl = databaseMetadataService.getConnectedDatabaseUrl();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String connectedUsername = authentication.getName();
            Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);

            List<Database> matchingDatabases = dbRepo.findAllByUrl(dbUrl);

            Database selectedDatabase = findSelectedDatabase(matchingDatabases, utilisateur);

            if (selectedDatabase == null) {
                String errorMessage = "No database found for the authenticated user or matching the URL (user ID: " + (utilisateur != null ? utilisateur.getId() : "N/A") + ")";
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", errorMessage));
            }

            // Now you can proceed with the selected database for executing the query
            List<Map<String, Object>> result = databaseMetadataService.executeQueriesAndReturnFirstResult(query);

            if (!result.isEmpty()) {
                Querys loggedQuery = createLoggedQuery(queryBuilders, queryName, selectedDatabase);
                queryRepo.save(loggedQuery);
            }

            // Delete duplicate queries
            try {
                deleteDuplicateQueries(queryName, queryBuilders, utilisateur, selectedDatabase);
            } catch (Exception ex) {
                // Handle the exception gracefully, e.g., log it
                System.err.println("Error deleting duplicate queries: " + ex.getMessage());
            }
            // Clear the set of unique queries since we executed the combined query
            uniqueQueries.clear();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String errorMessage = "Failed to execute the SQL query: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", errorMessage));
        }
    }*/
   @PostMapping("/query")
   public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> request) {
       try {
           String query = request.get("query");
           String queryBuilders = request.get("queryBuilders");
           String queryName = request.get("queryName");

           if (query == null || query.isEmpty() || queryBuilders == null || queryBuilders.isEmpty() || queryName == null || queryName.isEmpty()) {
               return ResponseEntity.badRequest().body("Missing or empty request parameters.");
           }

           // Execute the query and get the result
           List<Map<String, Object>> result = databaseMetadataService.executeQueriesAndReturnFirstResult(query);

           if (!result.isEmpty()) {
               return ResponseEntity.ok(result);
           } else {
               return ResponseEntity.notFound().build();
           }
       } catch (Exception e) {
           String errorMessage = "Failed to execute the SQL query: " + e.getMessage();
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", errorMessage));
       }
   }

    @PostMapping("/saveQuery")
    public ResponseEntity<?> saveQuery(@RequestBody Map<String, String> request) {
        System.out.println("first");
        try {
            String query = request.get("query");
            String queryBuilders = request.get("queryBuilders");
            String queryName = request.get("queryName");

            if (query == null || query.isEmpty() || queryBuilders == null || queryBuilders.isEmpty() || queryName == null || queryName.isEmpty()) {
                return ResponseEntity.badRequest().body("Missing or empty request parameters.");
            }

            uniqueQueries.add(query);

            // Get database information
            String dbUrl = databaseMetadataService.getConnectedDatabaseUrl();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String connectedUsername = authentication.getName();
            Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);
            List<Database> matchingDatabases = dbRepo.findAllByUrl(dbUrl);
            Database selectedDatabase = findSelectedDatabase(matchingDatabases, utilisateur);

            if (selectedDatabase == null) {
                String errorMessage = "No database found for the authenticated user or matching the URL (user ID: " + (utilisateur != null ? utilisateur.getId() : "N/A") + ")";
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", errorMessage));
            }

            // Save the query
            Querys loggedQuery = createLoggedQuery(queryBuilders, queryName, selectedDatabase);
            queryRepo.save(loggedQuery);

            // Delete duplicate queries
            try {
                deleteDuplicateQueries(queryName, queryBuilders, utilisateur, selectedDatabase);
            } catch (Exception ex) {
                // Handle the exception gracefully, e.g., log it
                System.err.println("Error deleting duplicate queries: " + ex.getMessage());
            }

            // Clear the set of unique queries since we executed the combined query
            uniqueQueries.clear();

            return ResponseEntity.ok("Query saved successfully.");
        } catch (Exception e) {
            String errorMessage = "Failed to save the query: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", errorMessage));
        }
    }




    private Database findSelectedDatabase(List<Database> matchingDatabases, Utilisateur utilisateur) {
        Database selectedDatabase = null;

        for (Database database : matchingDatabases) {
            if (database.getUtilisateur() != null && utilisateur != null &&
                    database.getUtilisateur().getId().equals(utilisateur.getId())) {
                selectedDatabase = database;
                break; // Found a match, no need to continue checking
            }
        }

        // If no matching database for the user, use the first database matching the URL
        if (selectedDatabase == null && !matchingDatabases.isEmpty()) {
            selectedDatabase = matchingDatabases.get(0);
        }

        return selectedDatabase;
    }


    private boolean isSuperset(String query) {
        for (String otherQuery : uniqueQueries) {
            if (!query.contains(otherQuery)) {
                return false;
            }
        }
        return true;
    }

    private Querys createLoggedQuery(String queryBuilders, String queryName, Database db) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String connectedUsername = authentication.getName();
        Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);

        Querys loggedQuery = new Querys();
        loggedQuery.setUser(utilisateur);
        loggedQuery.setConditions(queryBuilders);
        loggedQuery.setCreatedAt(LocalDateTime.now());
        loggedQuery.setName(queryName);
        loggedQuery.setDatabase(db);

        return loggedQuery;
    }




    @GetMapping("/queries")
    public ResponseEntity<?> getQueriesByDatabase() {
        try {
            String dbUrl = databaseMetadataService.getConnectedDatabaseUrl();
            Database db = dbRepo.findByUrl(dbUrl);
            Long dbId = db.getId();
            List<Querys> queries = queryRepo.findByDatabaseId(dbId);

            if (queries.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no Queries  found");
            }

            List<Querys> uniqueQueries = filterDuplicateQueries(queries);
            if (uniqueQueries.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no unique Queries  found");
            }

            return ResponseEntity.ok(uniqueQueries);
        } catch (Exception e) {
            // Log the exception here
            e.printStackTrace();
            String errorMessage = "Failed to fetch queries by database: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }



    private List<Querys> filterDuplicateQueries(List<Querys> queries) {
        Set<Querys> uniqueQueries = new HashSet<>();

        for (Querys query : queries) {
            boolean isDuplicate = false;

            for (Querys uniqueQuery : uniqueQueries) {
                if (query.getName().equals(uniqueQuery.getName()) &&
                        query.getConditions().equals(uniqueQuery.getConditions()) &&
                        query.getUser().equals(uniqueQuery.getUser()) &&
                        query.getDatabase().equals(uniqueQuery.getDatabase())) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                uniqueQueries.add(query);
            }
        }
        return new ArrayList<>(uniqueQueries);
    }



   /* @PostMapping("/executeMongoQuery")
    public ResponseEntity<?> executeMongoQueries(@RequestBody Map<String, Object> requestBody) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String connectedUsername = authentication.getName();
            Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);

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
                // Save the query in the database
                saveQuery(requestBody, utilisateur);

                return ResponseEntity.ok(resultsMap);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error parsing query JSON: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing queries: " + e.getMessage());
        }
    }*/

    @PostMapping("/executeMongoQuery")
    public ResponseEntity<?> executeMongoQuery(@RequestBody Map<String, Object> requestBody) {
        try {
            String connectionString = (String) requestBody.get("connectionString");
            String queryJson = (String) requestBody.get("query");

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
    @PostMapping("/saveMongoQuery")
    public ResponseEntity<?> saveMongoQuery(@RequestBody Map<String, Object> requestBody) {
        System.out.println("first");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String connectedUsername = authentication.getName();
            Utilisateur utilisateur = userRepository.findById(connectedUsername).orElse(null);
            System.out.println(requestBody);
            // Save the query in the database
            saveQuery(requestBody, utilisateur);

            return ResponseEntity.ok("Query saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving the query: " + e.getMessage());
        }
    }


    private void saveQuery(Map<String, Object> requestBody, Utilisateur utilisateur) {
        String queryName = (String) requestBody.get("queryName");
        String queryConditions = (String) requestBody.get("query");

        Querys query = new Querys();
        query.setName(queryName);
        query.setCreatedAt(LocalDateTime.now());
        query.setConditions(queryConditions);
        query.setUser(utilisateur); // Set the user associated with the query

        List<Database> matchingDatabases = dbRepo.findAllByUrl((String) requestBody.get("connectionString"));

        Database selectedDatabase = null;

        for (Database database : matchingDatabases) {
            if (database.getUtilisateur() != null && utilisateur != null &&
                    database.getUtilisateur().getId().equals(utilisateur.getId())) {
                selectedDatabase = database;
                break; // Found a match, no need to continue checking
            }
        }

        // If no matching database for the user, use the first database matching the URL
        if (selectedDatabase == null && !matchingDatabases.isEmpty()) {
            System.out.println("5RAAAAAA");
            selectedDatabase = matchingDatabases.get(0);
        }

        if (selectedDatabase != null) {
            query.setDatabase(selectedDatabase); // Set the database associated with the query
            queryRepo.save(query);
        }
        try {
            deleteDuplicateQueries(queryName, queryConditions, utilisateur, selectedDatabase);
        } catch (Exception ex) {
            // Handle the exception gracefully, e.g., log it
            System.err.println("Error deleting duplicate queries: " + ex.getMessage());
        }

    }

    private void deleteDuplicateQueries(String queryName, String queryConditions, Utilisateur utilisateur, Database selectedDatabase) {
        List<Querys> duplicateQueries = queryRepo.findDuplicateQueries(queryName, queryConditions, utilisateur, selectedDatabase);

        // Keep the first duplicate, delete the rest
        if (duplicateQueries.size() > 1) {
            for (int i = 1; i < duplicateQueries.size(); i++) {
                Querys duplicateQuery = duplicateQueries.get(i);
                queryRepo.delete(duplicateQuery);
            }
        }
    }



    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        System.out.println(url);
        try {
            databaseMetadataService.disconnect(url);
            return ResponseEntity.ok("Database connection closed successfully.");
        } catch (RuntimeException e) {
            String errorMessage = "Failed to disconnect from the database: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    private boolean isMongoDB(String url) {
        return url.startsWith("mongodb://");
    }

}
