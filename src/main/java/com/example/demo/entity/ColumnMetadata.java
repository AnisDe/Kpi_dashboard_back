package com.example.demo.entity;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity


public class ColumnMetadata {
    @Id
    @GeneratedValue
    private Long id;

    private String columnName;


    private String dataType;
    @ManyToOne
    private TableMetadata table;

    public ColumnMetadata(String columnName) {
        this.columnName = columnName;
    }

    public ColumnMetadata(Long id, String columnName) {
        this.id = id;
        this.columnName = columnName;
    }

    public ColumnMetadata() {

    }

    public TableMetadata getTable() {
        return table;
    }

    public void setTable(TableMetadata table) {
        this.table = table;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getters and setters
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}


