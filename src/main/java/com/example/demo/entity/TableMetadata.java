package com.example.demo.entity;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class TableMetadata {
    @Id
    @GeneratedValue
    private Long id;
    private String tableName;
    private String tableType;
    @OneToMany(mappedBy = "table")
    private List<ColumnMetadata> columns;

    public TableMetadata(String tableName, String tableType, List<ColumnMetadata> columns) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.columns = columns;
    }

    public TableMetadata() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMetadata> columns) {
        this.columns = columns;
    }
}
