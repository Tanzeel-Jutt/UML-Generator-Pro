package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a complete UML class diagram.
 * Contains all classes and their relationships.
 * Platform-independent: no UI dependencies.
 */
public class UMLDiagram {
    private String title;
    private String language;
    private List<UMLClass> classes;
    private List<UMLRelationship> relationships;

    public UMLDiagram() {
        this.classes = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    /**
     * Merge another diagram into this one.
     */
    public void merge(UMLDiagram other) {
        if (other == null) return;
        
        // Add classes, avoiding duplicates by name
        for (UMLClass otherClass : other.getClasses()) {
            if (this.classes.stream().noneMatch(c -> c.getName().equals(otherClass.getName()))) {
                this.addClass(otherClass);
            }
        }
        
        // Add relationships
        this.relationships.addAll(other.getRelationships());
    }

    public UMLDiagram(String title, String language) {
        this();
        this.title = title;
        this.language = language;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<UMLClass> getClasses() { return classes; }
    public void setClasses(List<UMLClass> classes) { this.classes = classes; }
    public void addClass(UMLClass umlClass) { this.classes.add(umlClass); }
    public List<UMLRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<UMLRelationship> relationships) { this.relationships = relationships; }
    public void addRelationship(UMLRelationship relationship) {
        if (!this.relationships.contains(relationship)) {
            this.relationships.add(relationship);
        }
    }

    /**
     * Find a class by name.
     */
    public Optional<UMLClass> findClass(String name) {
        return classes.stream().filter(c -> c.getName().equals(name)).findFirst();
    }

    /**
     * Get all relationships involving a specific class.
     */
    public List<UMLRelationship> getRelationshipsFor(String className) {
        return relationships.stream()
                .filter(r -> r.getSourceClassName().equals(className)
                        || r.getTargetClassName().equals(className))
                .toList();
    }

    /**
     * Get root classes (no parent class).
     */
    public List<UMLClass> getRootClasses() {
        return classes.stream()
                .filter(c -> c.getParentClass() == null || c.getParentClass().isEmpty())
                .toList();
    }

    /**
     * Get child classes of a given parent.
     */
    public List<UMLClass> getChildClasses(String parentName) {
        return classes.stream()
                .filter(c -> parentName.equals(c.getParentClass()))
                .toList();
    }

    /**
     * Determines if the diagram should use overview mode.
     * Overview mode is used when there are many classes (> 20).
     */
    public boolean shouldUseOverviewMode() {
        return classes.size() > 20;
    }

    /**
     * Get total count of all elements.
     */
    public int getTotalElementCount() {
        return classes.size() + relationships.size();
    }

    /**
     * Get all inheritance relationships.
     */
    public List<UMLRelationship> getInheritanceHierarchy() {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.INHERITANCE
                        || r.getType() == RelationshipType.IMPLEMENTATION)
                .toList();
    }

    @Override
    public String toString() {
        return "UMLDiagram{" + title + ", classes=" + classes.size()
                + ", relationships=" + relationships.size() + "}";
    }
}
