package com.umlgenerator.core.model;

/**
 * Represents a class attribute/field in UML.
 * Format: visibility name: Type [= defaultValue] {modifiers}
 */
public class UMLAttribute {
    private String name;
    private String type;
    private AccessModifier accessModifier;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isAbstract;
    private String defaultValue;

    // Builder pattern for clean construction
    public static class Builder {
        private final UMLAttribute attr = new UMLAttribute();

        public Builder name(String name) { attr.name = name; return this; }
        public Builder type(String type) { attr.type = type; return this; }
        public Builder accessModifier(AccessModifier am) { attr.accessModifier = am; return this; }
        public Builder isStatic(boolean s) { attr.isStatic = s; return this; }
        public Builder isFinal(boolean f) { attr.isFinal = f; return this; }
        public Builder isAbstract(boolean a) { attr.isAbstract = a; return this; }
        public Builder defaultValue(String dv) { attr.defaultValue = dv; return this; }

        public UMLAttribute build() {
            if (attr.accessModifier == null) attr.accessModifier = AccessModifier.PRIVATE;
            return attr;
        }
    }

    public UMLAttribute() {
        this.accessModifier = AccessModifier.PRIVATE;
    }

    public UMLAttribute(String name, String type, AccessModifier accessModifier) {
        this.name = name;
        this.type = type;
        this.accessModifier = accessModifier;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public AccessModifier getAccessModifier() { return accessModifier; }
    public void setAccessModifier(AccessModifier accessModifier) { this.accessModifier = accessModifier; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    /**
     * Returns the UML-formatted string representation.
     * Example: - name: String = "default" {static, final}
     */
    public String toUMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append(accessModifier.getSymbol()).append(" ");
        sb.append(name).append(": ").append(type);
        // Default values are no longer displayed in UML as per user request
        // if (defaultValue != null && !defaultValue.isEmpty()) {
        //     sb.append(" = ").append(defaultValue);
        // }
        // Modifiers
        StringBuilder mods = new StringBuilder();
        if (isStatic) mods.append("static");
        if (isFinal) {
            if (mods.length() > 0) mods.append(", ");
            mods.append("final");
        }
        if (isAbstract) {
            if (mods.length() > 0) mods.append(", ");
            mods.append("abstract");
        }
        if (mods.length() > 0) {
            sb.append(" {").append(mods).append("}");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toUMLString();
    }
}
