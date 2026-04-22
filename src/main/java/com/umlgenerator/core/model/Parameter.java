package com.umlgenerator.core.model;

/**
 * Represents a method parameter with name and type.
 * Immutable data class - platform independent.
 */
public class Parameter {
    private final String name;
    private final String type;
    private final String defaultValue;

    public Parameter(String name, String type) {
        this(name, type, null);
    }

    public Parameter(String name, String type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null && !defaultValue.isEmpty();
    }

    /**
     * Returns UML-formatted parameter string.
     * Format: name: Type [= defaultValue]
     */
    public String toUMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type);
        if (hasDefaultValue()) {
            sb.append(" = ").append(defaultValue);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toUMLString();
    }
}
