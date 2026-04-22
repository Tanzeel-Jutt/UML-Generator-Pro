package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a relationship between entities in an ER/EERD diagram.
 * Shown as a diamond shape connecting entities.
 */
public class ERRelationship {
    private String name;
    private String entity1Name;
    private String entity2Name;
    private String cardinality1; // "1", "N", "M"
    private String cardinality2;
    private String participation1; // "total" or "partial"
    private String participation2;
    private boolean isIdentifying; // Identifying relationship (double diamond)
    private List<ERAttribute> attributes; // Relationship attributes

    public ERRelationship() {
        this.attributes = new ArrayList<>();
        this.participation1 = "partial";
        this.participation2 = "partial";
    }

    public ERRelationship(String name, String entity1Name, String entity2Name,
                          String cardinality1, String cardinality2) {
        this();
        this.name = name;
        this.entity1Name = entity1Name;
        this.entity2Name = entity2Name;
        this.cardinality1 = cardinality1;
        this.cardinality2 = cardinality2;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEntity1Name() { return entity1Name; }
    public void setEntity1Name(String entity1Name) { this.entity1Name = entity1Name; }
    public String getEntity2Name() { return entity2Name; }
    public void setEntity2Name(String entity2Name) { this.entity2Name = entity2Name; }
    public String getCardinality1() { return cardinality1; }
    public void setCardinality1(String cardinality1) { this.cardinality1 = cardinality1; }
    public String getCardinality2() { return cardinality2; }
    public void setCardinality2(String cardinality2) { this.cardinality2 = cardinality2; }
    public String getParticipation1() { return participation1; }
    public void setParticipation1(String participation1) { this.participation1 = participation1; }
    public String getParticipation2() { return participation2; }
    public void setParticipation2(String participation2) { this.participation2 = participation2; }
    public boolean isIdentifying() { return isIdentifying; }
    public void setIdentifying(boolean identifying) { isIdentifying = identifying; }
    public List<ERAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<ERAttribute> attributes) { this.attributes = attributes; }
    public void addAttribute(ERAttribute attr) { this.attributes.add(attr); }

    /**
     * Determine the RelationshipType from cardinalities.
     */
    public RelationshipType getRelationshipType() {
        boolean isOne1 = "1".equals(cardinality1);
        boolean isOne2 = "1".equals(cardinality2);
        if (isOne1 && isOne2) return RelationshipType.ONE_TO_ONE;
        if (isOne1 || isOne2) return RelationshipType.ONE_TO_MANY;
        return RelationshipType.MANY_TO_MANY;
    }

    /**
     * Check if entity1 has total participation.
     */
    public boolean isEntity1TotalParticipation() {
        return "total".equalsIgnoreCase(participation1);
    }

    /**
     * Check if entity2 has total participation.
     */
    public boolean isEntity2TotalParticipation() {
        return "total".equalsIgnoreCase(participation2);
    }

    @Override
    public String toString() {
        return entity1Name + " --(" + name + ":" + cardinality1 + ":" + cardinality2 + ")--> " + entity2Name;
    }
}
