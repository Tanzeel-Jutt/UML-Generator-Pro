package com.umlgenerator.core.model;

/**
 * Represents the types of relationships between UML classes/entities.
 * Covers both class diagram and ER diagram relationship types.
 */
public enum RelationshipType {
    // UML Class Diagram relationships
    INHERITANCE("extends", "─▷"),
    IMPLEMENTATION("implements", "─ ─▷"),
    COMPOSITION("contains", "◆──"),
    AGGREGATION("has", "◇──"),
    ASSOCIATION("uses", "──"),
    DEPENDENCY("depends on", "- - ->"),

    // ER Diagram relationships
    ONE_TO_ONE("1:1", "1──1"),
    ONE_TO_MANY("1:N", "1──*"),
    MANY_TO_MANY("M:N", "*──*"),
    IDENTIFYING("identifying", "══"),
    SPECIALIZATION("ISA", "─△");

    private final String label;
    private final String symbol;

    RelationshipType(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    public String getLabel() {
        return label;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isERRelationship() {
        return this == ONE_TO_ONE || this == ONE_TO_MANY || this == MANY_TO_MANY
                || this == IDENTIFYING || this == SPECIALIZATION;
    }

    public boolean isUMLRelationship() {
        return !isERRelationship();
    }
}
