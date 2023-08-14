package com.example.demo.service;

import com.example.demo.entity.ColumnMetadata;
import com.example.demo.entity.TableMetadata;
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


    /**
     * Retrieves a list of all tables in the connected database
     *
     * @return a list of {@link TableMetadata} objects representing the tables in the database
     */
    JSONArray getSqlMetaData(String url, String username, String password);

    void startRealTimeUpdates(String url, String username, String password);

    void stopRealTimeUpdates();

    /**
     * Retrieves a list of all columns for the specified table in the connected database
     *
     * @return a list of {@link ColumnMetadata} objects representing the columns in the specified table
     */


    List<Object> getColumnValues(String url, String username, String password, String tableName, String columnName);



    Map<String, Map<String, Map<String, List<String>>>> buildTableInfoSql(String url, String username, String password, JSONArray tables);

    List<Map<String, Object>> executeQuery(String query);

    JSONArray getMongoMetaData(String url);


    Map<String, Map<String, Map<String, List<String>>>> buildTableInfoMongo(String connectionString, String databaseName, JSONArray collections);
}




