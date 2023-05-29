package com.example.demo.controller;

import com.example.demo.service.DatabaseMetadataService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/tables")
public class TableController {


    private final DatabaseMetadataService databaseMetadataService;

    public TableController(DatabaseMetadataService databaseMetadataService) {
        this.databaseMetadataService = databaseMetadataService;
    }


    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String username = request.get("username");
        String password = request.get("password");

        try {
            databaseMetadataService.connect(url, username, password);
            JSONArray tables = databaseMetadataService.getMetaData(url, username, password);
            Map<String, Map<String, List<String>>> result = buildTableInfo(url, username, password, tables);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            String errorMessage = "Failed to connect to the database: " + e.getMessage();
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    private Map<String, Map<String, List<String>>> buildTableInfo(String url, String username, String password, JSONArray tables) {
        Map<String, Map<String, List<String>>> result = new TreeMap<>();

        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            String tableName = table.getString("tableName");
            JSONArray columnsArray = table.getJSONArray("columns");

            Map<String, List<String>> tableInfo = new HashMap<>();

            for (int j = 0; j < columnsArray.length(); j++) {
                JSONObject column = columnsArray.getJSONObject(j);
                String columnName = column.getString("columnName");
                List<Object> columnValues = databaseMetadataService.getColumnValues(url, username, password, tableName, columnName);
                List<String> columnTextValues = new ArrayList<>();

                for (Object value : columnValues) {
                    columnTextValues.add(value.toString());
                }

                tableInfo.put(columnName, columnTextValues);
            }

            result.put(tableName, tableInfo);
        }

        return result;
    }



}
