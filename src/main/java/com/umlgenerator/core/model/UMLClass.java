package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class/interface/enum in UML class diagram.
 * Contains all attributes, methods, and metadata for rendering.
 * 
 * Platform-independent: no UI dependencies.
 */
public class UMLClass {
    private String name;
    private ClassType classType;
    private String packageName;
    private List<UMLAttribute> attributes;
    private List<UMLMethod> methods;
    private List<String> enumConstants; // For enum types
    private String parentClass; // extends
    private List<String> interfaces; // implements
    private AccessModifier classAccessModifier;

    public UMLClass() {
        this.attributes = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.enumConstants = new ArrayList<>();
        this.interfaces = new ArrayList<>();
        this.classType = ClassType.CLASS;
        this.classAccessModifier = AccessModifier.PUBLIC;
    }

    public UMLClass(String name, ClassType classType) {
        this();
        this.name = name;
        this.classType = classType;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ClassType getClassType() { return classType; }
    public void setClassType(ClassType classType) { this.classType = classType; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public List<UMLAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<UMLAttribute> attributes) { this.attributes = attributes; }
    public void addAttribute(UMLAttribute attribute) { this.attributes.add(attribute); }
    public List<UMLMethod> getMethods() { return methods; }
    public void setMethods(List<UMLMethod> methods) { this.methods = methods; }
    public void addMethod(UMLMethod method) { this.methods.add(method); }
    public List<String> getEnumConstants() { return enumConstants; }
    public void setEnumConstants(List<String> enumConstants) { this.enumConstants = enumConstants; }
    public void addEnumConstant(String constant) { this.enumConstants.add(constant); }
    public String getParentClass() { return parentClass; }
    public void setParentClass(String parentClass) { this.parentClass = parentClass; }
    public List<String> getInterfaces() { return interfaces; }
    public void setInterfaces(List<String> interfaces) { this.interfaces = interfaces; }
    public void addInterface(String iface) { this.interfaces.add(iface); }
    public AccessModifier getClassAccessModifier() { return classAccessModifier; }
    public void setClassAccessModifier(AccessModifier classAccessModifier) { this.classAccessModifier = classAccessModifier; }

    /**
     * Returns the display name with stereotype if applicable.
     * e.g., "<<abstract>>\nAnimalClass" or "<<interface>>\nDrawable"
     */
    public String getDisplayName() {
        if (classType.hasStereotype()) {
            return classType.getStereotype() + "\n" + name;
        }
        return name;
    }

    /**
     * Returns the full qualified name including package.
     */
    public String getFullyQualifiedName() {
        if (packageName != null && !packageName.isEmpty()) {
            return packageName + "." + name;
        }
        return name;
    }

    /**
     * Check if this class has any abstract methods.
     */
    public boolean hasAbstractMethods() {
        return methods.stream().anyMatch(UMLMethod::isAbstract);
    }

    /**
     * Get all constructor methods.
     */
    public List<UMLMethod> getConstructors() {
        return methods.stream().filter(UMLMethod::isConstructor).toList();
    }

    /**
     * Get all non-constructor methods.
     */
    public List<UMLMethod> getNonConstructorMethods() {
        return methods.stream().filter(m -> !m.isConstructor()).toList();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UMLClass umlClass = (UMLClass) o;
        return name != null && name.equals(umlClass.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
