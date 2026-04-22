package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a table in a relational schema mapping.
 * Generated from ER entities during relational mapping.
 */
public class RelationalTable {
    private String name;
    private List<RelationalColumn> columns;
    private String sourceEntityName; // Original ER entity

    public RelationalTable() {
        this.columns = new ArrayList<>();
    }

    public RelationalTable(String name) {
        this();
        this.name = name;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<RelationalColumn> getColumns() { return columns; }
    public void setColumns(List<RelationalColumn> columns) { this.columns = columns; }
    public void addColumn(RelationalColumn column) { this.columns.add(column); }
    public String getSourceEntityName() { return sourceEntityName; }
    public void setSourceEntityName(String sourceEntityName) { this.sourceEntityName = sourceEntityName; }

    /**
     * Get primary key columns.
     */
    public List<RelationalColumn> getPrimaryKeys() {
        return columns.stream().filter(RelationalColumn::isPrimaryKey).toList();
    }

    /**
     * Get foreign key columns.
     */
    public List<RelationalColumn> getForeignKeys() {
        return columns.stream().filter(RelationalColumn::isForeignKey).toList();
    }

    /**
     * Generate CREATE TABLE SQL statement.
     */
    public String toSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(name).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("    ").append(columns.get(i).toSQL());
            if (i < columns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(");");
        return sb.toString();
    }

    /**
     * Returns compact display string for relational mapping.
     * Format: TableName(PK_col, col1, col2, FK_col)
     */
    public String toMappingString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        String cols = columns.stream().map(c -> {
            String prefix = "";
            if (c.isPrimaryKey()) prefix = "PK:";
            else if (c.isForeignKey()) prefix = "FK:";
            return prefix + c.getName();
        }).collect(Collectors.joining(", "));
        sb.append(cols).append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toMappingString();
    }
}
