package com.example.demo.service;

import org.json.JSONArray;

import java.util.List;
import java.util.Map;


public interface DatabaseMetadataService {


    /**
     * Connects to the database using the provided credentials
     *
     * @param url      the URL of the database
     * @param username the username to use for authentication
     * @param password the password to use for authentication
     */
    void connect(String url, String username, String password);



    JSONArray getSqlMetaData(String url, String username, String password);

    void startRealTimeUpdates(String url, String username, String password);

    void stopRealTimeUpdates();




    List<Object> getColumnValues(String url, String username, String password, String tableName, String columnName);



    Map<String, Map<String, Map<String, List<String>>>> buildTableInfoSql(String url, String username, String password, JSONArray tables);


    List<Map<String, Object>> executeQueriesAndReturnFirstResult(String query);

    List<Map<String, Object>> executeQuery(String query);

    JSONArray getMongoMetaData(String url);


    Map<String, Map<String, Map<String, List<String>>>> buildTableInfoMongo(String connectionString, JSONArray collections);

    List<Map<String, Object>> executeMongoQueries(String connectionString, Map<String, Object> queryMap);
}




