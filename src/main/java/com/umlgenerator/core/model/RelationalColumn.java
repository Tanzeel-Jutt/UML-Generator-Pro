package com.umlgenerator.core.model;

/**
 * Represents a column in a relational mapping table.
 * Generated from ER/EERD diagram entities.
 */
public class RelationalColumn {
    private String name;
    private String dataType;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private boolean isNotNull;
    private boolean isUnique;
    private String defaultValue;
    private String referencedTable;
    private String referencedColumn;

    public RelationalColumn() {}

    public RelationalColumn(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    public RelationalColumn(String name, String dataType, boolean isPrimaryKey) {
        this(name, dataType);
        this.isPrimaryKey = isPrimaryKey;
        if (isPrimaryKey) this.isNotNull = true;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public void setPrimaryKey(boolean primaryKey) { isPrimaryKey = primaryKey; }
    public boolean isForeignKey() { return isForeignKey; }
    public void setForeignKey(boolean foreignKey) { isForeignKey = foreignKey; }
    public boolean isNotNull() { return isNotNull; }
    public void setNotNull(boolean notNull) { isNotNull = notNull; }
    public boolean isUnique() { return isUnique; }
    public void setUnique(boolean unique) { isUnique = unique; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getReferencedTable() { return referencedTable; }
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }
    public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }

    /**
     * Generate SQL column definition.
     */
    public String toSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(dataType);
        if (isPrimaryKey) sb.append(" PRIMARY KEY");
        if (isNotNull && !isPrimaryKey) sb.append(" NOT NULL");
        if (isUnique) sb.append(" UNIQUE");
        if (defaultValue != null) sb.append(" DEFAULT ").append(defaultValue);
        if (isForeignKey && referencedTable != null) {
            sb.append(" REFERENCES ").append(referencedTable)
              .append("(").append(referencedColumn != null ? referencedColumn : "id").append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return (isPrimaryKey ? "PK " : "") + (isForeignKey ? "FK " : "") + name + " : " + dataType;
    }
}
