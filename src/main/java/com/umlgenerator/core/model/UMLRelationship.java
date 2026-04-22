package com.umlgenerator.core.model;

/**
 * Represents a relationship between two UML classes.
 * Supports inheritance, implementation, composition, aggregation, association, and dependency.
 */
public class UMLRelationship {
    private String sourceClassName;
    private String targetClassName;
    private RelationshipType type;
    private String sourceMultiplicity;
    private String targetMultiplicity;
    private String label;

    public UMLRelationship() {}

    public UMLRelationship(String sourceClassName, String targetClassName, RelationshipType type) {
        this.sourceClassName = sourceClassName;
        this.targetClassName = targetClassName;
        this.type = type;
    }

    public UMLRelationship(String sourceClassName, String targetClassName, RelationshipType type,
                           String sourceMultiplicity, String targetMultiplicity, String label) {
        this(sourceClassName, targetClassName, type);
        this.sourceMultiplicity = sourceMultiplicity;
        this.targetMultiplicity = targetMultiplicity;
        this.label = label;
    }

    // Getters and setters
    public String getSourceClassName() { return sourceClassName; }
    public void setSourceClassName(String sourceClassName) { this.sourceClassName = sourceClassName; }
    public String getTargetClassName() { return targetClassName; }
    public void setTargetClassName(String targetClassName) { this.targetClassName = targetClassName; }
    public RelationshipType getType() { return type; }
    public void setType(RelationshipType type) { this.type = type; }
    public String getSourceMultiplicity() { return sourceMultiplicity; }
    public void setSourceMultiplicity(String sourceMultiplicity) { this.sourceMultiplicity = sourceMultiplicity; }
    public String getTargetMultiplicity() { return targetMultiplicity; }
    public void setTargetMultiplicity(String targetMultiplicity) { this.targetMultiplicity = targetMultiplicity; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    /**
     * Returns a description of this relationship for display.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceClassName).append(" ").append(type.getLabel()).append(" ").append(targetClassName);
        if (label != null && !label.isEmpty()) {
            sb.append(" (").append(label).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UMLRelationship that = (UMLRelationship) o;
        return sourceClassName.equals(that.sourceClassName)
                && targetClassName.equals(that.targetClassName)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = sourceClassName.hashCode();
        result = 31 * result + targetClassName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
