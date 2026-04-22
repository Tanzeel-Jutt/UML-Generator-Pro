package com.umlgenerator.core.model;

/**
 * Represents the type of a class in UML notation.
 * Each type has its standard UML stereotype label.
 */
public enum ClassType {
    CLASS(""),
    ABSTRACT_CLASS("<<abstract>>"),
    INTERFACE("<<interface>>"),
    ENUM("<<enumeration>>"),
    RECORD("<<record>>"),
    ANNOTATION("<<annotation>>");

    private final String stereotype;

    ClassType(String stereotype) {
        this.stereotype = stereotype;
    }

    public String getStereotype() {
        return stereotype;
    }

    public boolean hasStereotype() {
        return !stereotype.isEmpty();
    }
}
