package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an attribute in an ER/EERD diagram.
 * Supports primary keys, foreign keys, multi-valued attributes,
 * derived attributes, and composite attributes.
 */
public class ERAttribute {
    private String name;
    private String dataType;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private boolean isMultiValued;    // EERD: shown with double ellipse
    private boolean isDerived;        // EERD: shown with dashed ellipse
    private boolean isComposite;     // EERD: has sub-attributes
    private boolean isUnique;
    private boolean isNotNull;
    private String defaultValue;
    private List<ERAttribute> subAttributes; // For composite attributes
    private String referencedTable;  // FK reference
    private String referencedColumn; // FK reference column

    public ERAttribute() {
        this.subAttributes = new ArrayList<>();
    }

    public ERAttribute(String name, String dataType) {
        this();
        this.name = name;
        this.dataType = dataType;
    }

    public ERAttribute(String name, String dataType, boolean isPrimaryKey) {
        this(name, dataType);
        this.isPrimaryKey = isPrimaryKey;
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
    public boolean isMultiValued() { return isMultiValued; }
    public void setMultiValued(boolean multiValued) { isMultiValued = multiValued; }
    public boolean isDerived() { return isDerived; }
    public void setDerived(boolean derived) { isDerived = derived; }
    public boolean isComposite() { return isComposite; }
    public void setComposite(boolean composite) { isComposite = composite; }
    public boolean isUnique() { return isUnique; }
    public void setUnique(boolean unique) { isUnique = unique; }
    public boolean isNotNull() { return isNotNull; }
    public void setNotNull(boolean notNull) { isNotNull = notNull; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public List<ERAttribute> getSubAttributes() { return subAttributes; }
    public void setSubAttributes(List<ERAttribute> subAttributes) { this.subAttributes = subAttributes; }
    public void addSubAttribute(ERAttribute subAttr) { this.subAttributes.add(subAttr); this.isComposite = true; }
    public String getReferencedTable() { return referencedTable; }
    public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }
    public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }

    /**
     * Returns display label with key indicators.
     * PK attributes are underlined, FK attributes marked.
     */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        if (isPrimaryKey) sb.append("PK ");
        if (isForeignKey) sb.append("FK ");
        sb.append(name);
        if (dataType != null) sb.append(" : ").append(dataType);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
