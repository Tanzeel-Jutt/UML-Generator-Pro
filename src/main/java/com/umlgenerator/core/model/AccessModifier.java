package com.umlgenerator.core.model;

/**
 * Represents UML access modifiers with their standard UML symbols.
 * Platform-independent: safe for Android migration.
 */
public enum AccessModifier {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE_PRIVATE("");

    private final String symbol;

    AccessModifier(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Parse access modifier from Java/C++ keyword.
     */
    public static AccessModifier fromKeyword(String keyword) {
        if (keyword == null) return PACKAGE_PRIVATE;
        return switch (keyword.trim().toLowerCase()) {
            case "public" -> PUBLIC;
            case "private" -> PRIVATE;
            case "protected" -> PROTECTED;
            default -> PACKAGE_PRIVATE;
        };
    }

    /**
     * Parse access modifier from Python convention.
     * Python uses _ prefix for protected, __ for private.
     */
    public static AccessModifier fromPythonName(String name) {
        if (name == null) return PUBLIC;
        if (name.startsWith("__") && !name.endsWith("__")) return PRIVATE;
        if (name.startsWith("_")) return PROTECTED;
        return PUBLIC;
    }
}
