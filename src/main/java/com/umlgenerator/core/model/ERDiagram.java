package com.umlgenerator.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a complete ER or EERD diagram.
 * Contains entities, relationships, and supports both standard ER and Enhanced ER features.
 */
public class ERDiagram {
    private String title;
    private List<EREntity> entities;
    private List<ERRelationship> relationships;
    private boolean isEnhanced; // EERD features enabled

    public ERDiagram() {
        this.entities = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    public ERDiagram(String title) {
        this();
        this.title = title;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<EREntity> getEntities() { return entities; }
    public void setEntities(List<EREntity> entities) { this.entities = entities; }
    public void addEntity(EREntity entity) { this.entities.add(entity); }
    public List<ERRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<ERRelationship> relationships) { this.relationships = relationships; }
    public void addRelationship(ERRelationship relationship) { this.relationships.add(relationship); }
    public boolean isEnhanced() { return isEnhanced; }
    public void setEnhanced(boolean enhanced) { isEnhanced = enhanced; }

    /**
     * Find an entity by name.
     */
    public Optional<EREntity> findEntity(String name) {
        return entities.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Get all relationships involving a specific entity.
     */
    public List<ERRelationship> getRelationshipsFor(String entityName) {
        return relationships.stream()
                .filter(r -> r.getEntity1Name().equalsIgnoreCase(entityName)
                        || r.getEntity2Name().equalsIgnoreCase(entityName))
                .toList();
    }

    /**
     * Get weak entities.
     */
    public List<EREntity> getWeakEntities() {
        return entities.stream().filter(EREntity::isWeak).toList();
    }

    /**
     * Get entities that have specialization (EERD).
     */
    public List<EREntity> getSpecializedEntities() {
        return entities.stream().filter(EREntity::hasSpecialization).toList();
    }

    /**
     * Auto-detect if EERD features are present.
     */
    public boolean hasEERDFeatures() {
        return entities.stream().anyMatch(e -> e.hasSpecialization() || e.isSpecializedChild())
                || entities.stream().flatMap(e -> e.getAttributes().stream())
                .anyMatch(a -> a.isMultiValued() || a.isDerived() || a.isComposite());
    }

    @Override
    public String toString() {
        return "ERDiagram{" + title + ", entities=" + entities.size()
                + ", relationships=" + relationships.size() + "}";
    }
}
