package com.example.demo.service;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class DatabaseMetadataServiceImpl implements DatabaseMetadataService {
    private Connection connection;
    private static String mongoDatabaseName;


    private static MongoClient client;
    private String url;
    private String username;
    private String password;
    private MetadataUpdateThread updateThread;


    @Override
    public void connect(String url, String username, String password ) {

        try {
            this.url = url;
            this.username = username;
            this.password = password;

            if (url.startsWith("jdbc:postgresql://")) {
                // Connect to MySQL
                connectToSQL(url, username, password);
            } else if (url.startsWith("mongodb://")) {
                // Connect to MongoDB
                connectToMongoDB(url);
            } else {
                throw new IllegalArgumentException("Unsupported database URL");
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to the database:");
            e.printStackTrace();
            Throwable rootCause = e.getCause();
            if (rootCause != null) {
                System.err.println("Root cause:");
                rootCause.printStackTrace();
            }
            throw new RuntimeException("Unable to connect to the database", e);
        }

    }

    private void connectToSQL(String url, String username, String password) throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
        System.out.println("Connected to SQL database");
    }

    private static void connectToMongoDB(String url) {
        // Create a connection string from the provided URL
        ConnectionString connString = new ConnectionString(url);

        // Create a MongoClient instance
         client = MongoClients.create(connString);
        mongoDatabaseName = connString.getDatabase();
        // Access the specified database
        MongoDatabase database = client.getDatabase(connString.getDatabase());
        // List all collections in the database
        ListCollectionsIterable<Document> collections = database.listCollections();

        // Iterate through the collections and print their names
        try {
            // Iterate through the collections and print their names
            if (!collections.iterator().hasNext()) {
                System.out.println("The database has no collections.");
            } else {
                // Iterate through the collections and print their names
                collections.forEach(collectionInfo -> {
                    String collectionName = collectionInfo.getString("name");
                });
            }
        } catch (Throwable throwable) {
            System.err.println("Exception during MongoDB connection:");
            throwable.printStackTrace();

        }
    }

    @Override
    public String getConnectedDatabaseName() {
        if (connection != null) {
            // For PostgreSQL
            if (url.startsWith("jdbc:postgresql://")) {
                try {
                    return connection.getCatalog();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            // For MongoDB
            else if (url.startsWith("mongodb://")) {
                return mongoDatabaseName;
            }
        }
        return null; // Return null if not connected or unsupported database
    }

    @Override
    public String getConnectedDatabaseUrl() {
        if (connection != null || client != null) {
            return url;
        } else {
            return null;
        }
    }


    public JSONArray getMongoMetaData(String connectionString) {
        JSONArray result = new JSONArray();

        try {
            ConnectionString connString = new ConnectionString(connectionString);
            MongoDatabase database = client.getDatabase(connString.getDatabase());

            for (String collectionName : database.listCollectionNames()) {
                JSONObject collectionObject = new JSONObject();
                collectionObject.put("collectionName", collectionName);

                MongoCollection<Document> collection = database.getCollection(collectionName);
                Document sampleDocument = collection.find().first();
                if (sampleDocument != null) {
                    JSONObject fieldsObject = new JSONObject();
                    for (String fieldName : sampleDocument.keySet()) {
                        Object fieldValue = sampleDocument.get(fieldName);
                        String fieldType = fieldValue != null ? fieldValue.getClass().getSimpleName() : "test";
                        fieldsObject.put(fieldName, fieldType);
                    }
                    collectionObject.put("fields", fieldsObject);
                }

                result.put(collectionObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }





    @Override
    public JSONArray getSqlMetaData(String url, String username, String password) {
        JSONArray result = new JSONArray();

        try {

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
                    int dataType = columnResultSet.getInt("DATA_TYPE");
                    String columnType = getColumnType(dataType);
                    JSONObject column = new JSONObject();
                    column.put("columnName", columnName);
                    column.put("columnType", columnType);
                    columns.put(column);
                }
                table.put("columns", columns);
                result.put(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



    @Override
    public void startRealTimeUpdates(String url, String username, String password) {
        if (updateThread != null && updateThread.isAlive()) {
            return;
        }

        updateThread = new MetadataUpdateThread(url, username, password);
        updateThread.start();
    }

    @Override
    public void stopRealTimeUpdates() {
        if (updateThread != null) {
            updateThread.stopUpdating();
        }
    }

    @Override
    public List<Object> getColumnValues(String url, String username, String password, String tableName, String columnName) {
        List<Object> columnValues = new ArrayList<>();

        try {
            connect(url, username, password);

            Statement statement = connection.createStatement();
            String query = "SELECT \"" + columnName + "\" FROM \"" + tableName + "\"";
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                columnValues.add(resultSet.getObject(columnName));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columnValues;
    }

    @Override
    public Map<String, Map<String, Map<String, List<String>>>> buildTableInfoSql(String url, String username, String password, JSONArray tables) {
        Map<String, Map<String, Map<String, List<String>>>> result = new TreeMap<>();

        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            String tableName = table.getString("tableName");
            JSONArray columnsArray = table.getJSONArray("columns");

            Map<String, Map<String, List<String>>> tableInfo = new HashMap<>();

            for (int j = 0; j < columnsArray.length(); j++) {
                JSONObject column = columnsArray.getJSONObject(j);
                String columnName = column.getString("columnName");
                String columnType = column.getString("columnType");

                List<Object> columnValues = this.getColumnValues(url, username, password, tableName, columnName);

                List<String> columnTextValues = columnValues.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());

                Map<String, List<String>> columnInfo = new HashMap<>();
                columnInfo.put("columnType", Collections.singletonList(columnType));
                columnInfo.put("columnValues", columnTextValues);

                tableInfo.put(columnName, columnInfo);
            }

            result.put(tableName, tableInfo);
        }
        return result;
    }
    @Override
    public List<Map<String, Object>> executeQueriesAndReturnFirstResult(String query) {
        System.out.println(query);

        String[] queries = query.split(";");
        for (String individualQuery : queries) {
            individualQuery = individualQuery.trim();
            if (!individualQuery.isEmpty()) {
                List<Map<String, Object>> result = executeQuery(individualQuery);
                if (!result.isEmpty()) {
                    return result; // Return the first non-empty result immediately
                }
            }
        }

        return null; // Return null if no results are found
    }


    @Override
    public List<Map<String, Object>> executeQuery(String query) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object columnValue = resultSet.getObject(i);
                    row.put(columnName, columnValue);
                }
                result.add(row);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



    private String getColumnType(int dataType) {
        switch (dataType) {
            case Types.BIGINT:
                return "BIGINT";
            case Types.BINARY:
                return "BINARY";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.CHAR:
                return "CHAR";
            case Types.DATE:
                return "DATE";
            case Types.DECIMAL:
                return "DECIMAL";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.FLOAT:
                return "FLOAT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.NUMERIC:
                return "NUMERIC";
            case Types.TIME:
                return "TIME";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.VARCHAR:
                return "VARCHAR";
            default:
                return "Unknown";
        }
    }

    private class MetadataUpdateThread extends Thread {
        private final String url;
        private final String username;
        private final String password;
        private volatile boolean running;

        public MetadataUpdateThread(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.running = true;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    getSqlMetaData(url, username, password);
                    System.out.println("data refreshed");

                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopUpdating() {
            running = false;
        }
    }

    @Override
    public Map<String, Map<String, Map<String, List<String>>>> buildTableInfoMongo(String connectionString, JSONArray collections) {
        Map<String, Map<String, Map<String, List<String>>>> result = new TreeMap<>();

        try {
            ConnectionString connString = new ConnectionString(connectionString);
            MongoDatabase database = client.getDatabase(connString.getDatabase());

            for (int i = 0; i < collections.length(); i++) {
                JSONObject collectionObj = collections.getJSONObject(i);

                if (!collectionObj.has("collectionName") || !collectionObj.has("fields")) {
                    continue;
                }

                String collectionName = collectionObj.getString("collectionName");
                JSONObject fieldsObj = collectionObj.getJSONObject("fields");

                Map<String, Map<String, List<String>>> collectionInfo = new HashMap<>();
                MongoCollection<Document> collection = database.getCollection(collectionName);

                Iterator<String> fieldNames = fieldsObj.keys();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    String fieldType = fieldsObj.getString(fieldName);

                    List<String> fieldTextValues = getFieldValuesMongo(collection, fieldName);

                    Map<String, List<String>> fieldInfo = new HashMap<>();
                    fieldInfo.put("fieldType", Collections.singletonList(fieldType));
                    fieldInfo.put("fieldValues", fieldTextValues);

                    collectionInfo.put(fieldName, fieldInfo);
                }

                result.put(collectionName, collectionInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MongoException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> executeMongoQueries(String connectionString, Map<String, Object> queryMap) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            ConnectionString connString = new ConnectionString(connectionString);
            MongoDatabase database = client.getDatabase(connString.getDatabase());

            Map<String, Object> queries = (Map<String, Object>) queryMap.get("queries");

            for (Map.Entry<String, Object> entry : queries.entrySet()) {
                Object conditionsObject = entry.getValue();

                try {
                    BasicDBObject conditionQuery = new BasicDBObject((Map<String, Object>) conditionsObject);

                    MongoCollection<Document> collection = database.getCollection(entry.getKey());
                    FindIterable<Document> queryResult = collection.find(conditionQuery);

                    for (Document document : queryResult) {
                        Map<String, Object> resultMap = new HashMap<>(document);
                        resultList.add(resultMap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }




    private List<String> getFieldValuesMongo(MongoCollection<Document> collection, String fieldName) {
        List<String> fieldTextValues = new ArrayList<>();

        collection.find().forEach((Document document) -> {
            if (document.containsKey(fieldName)) {
                Object fieldValue = document.get(fieldName);
                fieldTextValues.add(fieldValue.toString());
            }
        });

        return fieldTextValues;
    }


    @Override
    public void disconnect(String url) {
        try {
            if (connection != null) {
                System.out.println(connection);
                connection.close();
                System.out.println("Disconnected from SQL database");
                System.out.println(connection);

            }

            // Close the MongoDB client explicitly.
            if (url.startsWith("mongodb://")) {

                // Create a MongoClient instance
                if (client != null) {
                    System.out.println(client);
                    client.close();
                    System.out.println("Disconnected from MongoDB");

                }
            }
        } catch (SQLException | MongoException e) {
            e.printStackTrace();
        }
    }

}
