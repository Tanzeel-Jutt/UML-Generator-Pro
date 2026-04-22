package com.umlgenerator.core.generator;

import com.umlgenerator.core.model.*;

import java.util.List;

/**
 * Generates Enhanced ER (EERD) Diagrams.
 * Extends ER diagrams with specialization/generalization, multi-valued attributes,
 * derived attributes, and composite attributes.
 */
public class EERDDiagramGenerator extends DiagramGenerator<ERDiagram> {

    @Override
    public String getGeneratorName() {
        return "Enhanced ER Diagram Generator";
    }

    @Override
    protected ERDiagram doGenerate(Object input) {
        if (input instanceof ERDiagram diagram) {
            diagram.setEnhanced(true);
            return enhanceDiagram(diagram);
        }
        return new ERDiagram("Empty EERD");
    }

    /**
     * Enhance a standard ER diagram with EERD features.
     */
    private ERDiagram enhanceDiagram(ERDiagram diagram) {
        // Detect specialization hierarchies
        detectSpecialization(diagram);

        // Detect multi-valued attributes (columns with array types or junction tables)
        detectMultiValuedAttributes(diagram);

        // Detect derived attributes (computed columns)
        detectDerivedAttributes(diagram);

        return diagram;
    }

    /**
     * Detect ISA (specialization/generalization) relationships.
     * Pattern: Multiple tables with PKs that are FKs to the same parent table.
     */
    private void detectSpecialization(ERDiagram diagram) {
        for (EREntity entity : diagram.getEntities()) {
            if (entity.hasSpecialization()) {
                // Already detected by SQL parser
                continue;
            }

            // Check if multiple entities reference this entity's PK
            List<String> potentialChildren = diagram.getEntities().stream()
                    .filter(e -> !e.equals(entity))
                    .filter(e -> e.getAttributes().stream()
                            .anyMatch(a -> a.isForeignKey() && a.isPrimaryKey()
                                    && entity.getName().equalsIgnoreCase(a.getReferencedTable())))
                    .map(EREntity::getName)
                    .toList();

            if (potentialChildren.size() >= 2) {
                entity.setChildEntities(new java.util.ArrayList<>(potentialChildren));
                entity.setDisjointSpecialization(true);
                entity.setTotalSpecialization(false);

                for (String childName : potentialChildren) {
                    diagram.findEntity(childName).ifPresent(child -> {
                        child.setParentEntity(entity.getName());
                    });
                }
            }
        }
    }

    /**
     * Detect multi-valued attributes.
     * Pattern: Separate table with FK to main entity + one value column.
     */
    private void detectMultiValuedAttributes(ERDiagram diagram) {
        List<EREntity> toRemove = new java.util.ArrayList<>();

        for (EREntity entity : diagram.getEntities()) {
            List<ERAttribute> attrs = entity.getAttributes();
            List<ERAttribute> fks = entity.getForeignKeys();

            // If table has only FK(s) + one or two non-FK columns, it might be multi-valued
            if (fks.size() == 1 && attrs.size() <= 3) {
                ERAttribute fk = fks.get(0);
                List<ERAttribute> nonFKAttrs = attrs.stream()
                        .filter(a -> !a.isForeignKey() && !a.isPrimaryKey())
                        .toList();

                if (nonFKAttrs.size() == 1) {
                    // This looks like a multi-valued attribute table
                    diagram.findEntity(fk.getReferencedTable()).ifPresent(parent -> {
                        ERAttribute multiValued = new ERAttribute(
                                nonFKAttrs.get(0).getName(),
                                nonFKAttrs.get(0).getDataType());
                        multiValued.setMultiValued(true);
                        parent.addAttribute(multiValued);
                    });
                    // Mark for potential removal from main entity list
                    // (keeping it for now for completeness)
                }
            }
        }
    }

    /**
     * Detect derived attributes.
     * Pattern: Column names starting with "total_", "avg_", "count_", etc.
     */
    private void detectDerivedAttributes(ERDiagram diagram) {
        String[] derivedPrefixes = {"total_", "avg_", "count_", "sum_", "max_", "min_", "calculated_", "computed_"};

        for (EREntity entity : diagram.getEntities()) {
            for (ERAttribute attr : entity.getAttributes()) {
                for (String prefix : derivedPrefixes) {
                    if (attr.getName().toLowerCase().startsWith(prefix)) {
                        attr.setDerived(true);
                        break;
                    }
                }
                // Also check for "age" which is commonly derived
                if (attr.getName().equalsIgnoreCase("age")) {
                    attr.setDerived(true);
                }
            }
        }
    }
}
