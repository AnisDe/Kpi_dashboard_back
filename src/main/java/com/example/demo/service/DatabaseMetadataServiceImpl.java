package com.example.demo.service;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


@Service
public class DatabaseMetadataServiceImpl implements DatabaseMetadataService {

    Connection connection;
    @Override
    public void connect(String url, String username, String password) {
        try {
            System.out.println("Welcome");
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database");
        }
    }

    @Override
    public JSONArray getMetaData(String url, String username, String password) {
        JSONArray result = new JSONArray();

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            DatabaseMetaData metadata = connection.getMetaData();
            ResultSet tableResultSet = metadata.getTables(null, null, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString("TABLE_NAME");
                JSONObject table = new JSONObject();
                table.put("tableName", tableName);
                JSONArray columns = new JSONArray();
                ResultSet columnResultSet = metadata.getColumns(null, null, tableName, null);
                while (columnResultSet.next()) {
                    String columnName = columnResultSet.getString("COLUMN_NAME");
                    JSONObject column = new JSONObject();
                    column.put("columnName", columnName);
                    columns.put(column);
                }
                table.put("columns", columns);
                result.put(table);
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



    @Override
    public List<Object> getColumnValues(String url, String username, String password, String tableName, String columnName) {
        List<Object> columnValues = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {

            String query = "SELECT \"" + columnName + "\" FROM \"" + tableName + "\"";

            try (ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    columnValues.add(resultSet.getObject(columnName));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columnValues;
    }






}

