package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entity in an ER/EERD diagram.
 * Entities are shown as rectangles (strong) or double rectangles (weak).
 */
public class EREntity {
    private String name;
    private List<ERAttribute> attributes;
    private boolean isWeak;          // Weak entity (double rectangle)
    private String parentEntity;     // For EERD specialization (ISA)
    private List<String> childEntities; // For EERD specialization
    private boolean isTotalSpecialization;   // EERD: total vs partial
    private boolean isDisjointSpecialization; // EERD: disjoint vs overlapping

    public EREntity() {
        this.attributes = new ArrayList<>();
        this.childEntities = new ArrayList<>();
    }

    public EREntity(String name) {
        this();
        this.name = name;
    }

    public EREntity(String name, boolean isWeak) {
        this(name);
        this.isWeak = isWeak;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ERAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<ERAttribute> attributes) { this.attributes = attributes; }
    public void addAttribute(ERAttribute attribute) { this.attributes.add(attribute); }
    public boolean isWeak() { return isWeak; }
    public void setWeak(boolean weak) { isWeak = weak; }
    public String getParentEntity() { return parentEntity; }
    public void setParentEntity(String parentEntity) { this.parentEntity = parentEntity; }
    public List<String> getChildEntities() { return childEntities; }
    public void setChildEntities(List<String> childEntities) { this.childEntities = childEntities; }
    public void addChildEntity(String child) { this.childEntities.add(child); }
    public boolean isTotalSpecialization() { return isTotalSpecialization; }
    public void setTotalSpecialization(boolean totalSpecialization) { isTotalSpecialization = totalSpecialization; }
    public boolean isDisjointSpecialization() { return isDisjointSpecialization; }
    public void setDisjointSpecialization(boolean disjointSpecialization) { isDisjointSpecialization = disjointSpecialization; }

    /**
     * Get the primary key attributes.
     */
    public List<ERAttribute> getPrimaryKeys() {
        return attributes.stream().filter(ERAttribute::isPrimaryKey).toList();
    }

    /**
     * Get all foreign key attributes.
     */
    public List<ERAttribute> getForeignKeys() {
        return attributes.stream().filter(ERAttribute::isForeignKey).toList();
    }

    /**
     * Check if this entity has specialization (EERD).
     */
    public boolean hasSpecialization() {
        return !childEntities.isEmpty();
    }

    /**
     * Check if this entity is a child in a specialization.
     */
    public boolean isSpecializedChild() {
        return parentEntity != null && !parentEntity.isEmpty();
    }

    @Override
    public String toString() {
        return (isWeak ? "[[" : "[") + name + (isWeak ? "]]" : "]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EREntity entity = (EREntity) o;
        return name != null && name.equals(entity.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
