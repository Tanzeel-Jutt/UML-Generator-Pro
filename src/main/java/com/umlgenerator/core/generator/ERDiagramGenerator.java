package com.umlgenerator.core.generator;

import com.umlgenerator.core.model.*;

/**
 * Generates ER (Entity-Relationship) Diagrams from SQL schemas.
 * Processes parsed SQL tables into proper ER notation.
 */
public class ERDiagramGenerator extends DiagramGenerator<ERDiagram> {

    @Override
    public String getGeneratorName() {
        return "ER Diagram Generator";
    }

    @Override
    protected ERDiagram doGenerate(Object input) {
        if (input instanceof ERDiagram diagram) {
            return organizeDiagram(diagram);
        }
        return new ERDiagram("Empty ER Diagram");
    }

    /**
     * Organize ER diagram for optimal display.
     */
    private ERDiagram organizeDiagram(ERDiagram diagram) {
        // Detect weak entities based on identifying relationships
        for (ERRelationship rel : diagram.getRelationships()) {
            if (rel.isIdentifying()) {
                diagram.findEntity(rel.getEntity1Name()).ifPresent(e -> {
                    if (e.getPrimaryKeys().stream().anyMatch(ERAttribute::isForeignKey)) {
                        e.setWeak(true);
                    }
                });
            }
        }

        return diagram;
    }

    @Override
    protected void postProcess(ERDiagram result) {
        // Validate all relationship entities exist
        for (ERRelationship rel : result.getRelationships()) {
            if (result.findEntity(rel.getEntity1Name()).isEmpty()) {
                EREntity missing = new EREntity(rel.getEntity1Name());
                result.addEntity(missing);
            }
            if (result.findEntity(rel.getEntity2Name()).isEmpty()) {
                EREntity missing = new EREntity(rel.getEntity2Name());
                result.addEntity(missing);
            }
        }
    }
}
