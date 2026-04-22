package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a method/operation in a UML class.
 * Format: visibility name(params): ReturnType {modifiers}
 * Abstract methods display as: + methodName() <<abstract>>
 */
public class UMLMethod {
    private String name;
    private String returnType;
    private AccessModifier accessModifier;
    private List<Parameter> parameters;
    private boolean isStatic;
    private boolean isAbstract;
    private boolean isFinal;
    private boolean isConstructor;
    private String body; // Stores method body for AST/Deep Class Scanning

    // Builder pattern
    public static class Builder {
        private final UMLMethod method = new UMLMethod();

        public Builder name(String name) { method.name = name; return this; }
        public Builder returnType(String type) { method.returnType = type; return this; }
        public Builder accessModifier(AccessModifier am) { method.accessModifier = am; return this; }
        public Builder addParameter(Parameter p) { method.parameters.add(p); return this; }
        public Builder parameters(List<Parameter> params) { method.parameters = new ArrayList<>(params); return this; }
        public Builder isStatic(boolean s) { method.isStatic = s; return this; }
        public Builder isAbstract(boolean a) { method.isAbstract = a; return this; }
        public Builder isFinal(boolean f) { method.isFinal = f; return this; }
        public Builder isConstructor(boolean c) { method.isConstructor = c; return this; }

        public UMLMethod build() {
            if (method.accessModifier == null) method.accessModifier = AccessModifier.PUBLIC;
            return method;
        }
    }

    public UMLMethod() {
        this.parameters = new ArrayList<>();
        this.accessModifier = AccessModifier.PUBLIC;
        this.returnType = "void";
    }

    public UMLMethod(String name, String returnType, AccessModifier accessModifier) {
        this();
        this.name = name;
        this.returnType = returnType;
        this.accessModifier = accessModifier;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public AccessModifier getAccessModifier() { return accessModifier; }
    public void setAccessModifier(AccessModifier accessModifier) { this.accessModifier = accessModifier; }
    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }
    public void addParameter(Parameter parameter) { this.parameters.add(parameter); }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }
    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    public boolean isConstructor() { return isConstructor; }
    public void setConstructor(boolean constructor) { isConstructor = constructor; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    /**
     * Detect if this method is a getter (getName, isActive, hasValue).
     */
    public boolean isGetter() {
        if (name == null || isConstructor) return false;
        return (name.startsWith("get") || name.startsWith("is") || name.startsWith("has"))
                && parameters.isEmpty()
                && returnType != null && !returnType.equals("void");
    }

    /**
     * Detect if this method is a setter (setName, setValue).
     */
    public boolean isSetter() {
        if (name == null || isConstructor) return false;
        return name.startsWith("set")
                && parameters.size() == 1
                && (returnType == null || returnType.equals("void"));
    }

    /**
     * Returns UML-formatted method string.
     * Example: + getName(id: int): String <<abstract>>
     */
    public String toUMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append(accessModifier.getSymbol()).append(" ");

        sb.append(name).append("(");

        // Parameters
        String paramStr = parameters.stream()
                .map(Parameter::toUMLString)
                .collect(Collectors.joining(", "));
        sb.append(paramStr).append(")");

        // Return type (show for all non-constructors, including void)
        if (!isConstructor && returnType != null) {
            sb.append(": ").append(returnType);
        }

        // Abstract label
        if (isAbstract) {
            sb.append(" <<abstract>>");
        }

        // Static underline note
        if (isStatic) {
            sb.append(" {static}");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return toUMLString();
    }
}
