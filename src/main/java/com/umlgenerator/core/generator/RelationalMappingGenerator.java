package com.umlgenerator.core.generator;

import com.umlgenerator.core.model.*;

import java.util.*;

/**
 * Generates Relational Schema Mapping from ER/EERD diagrams.
 * Implements the standard ER-to-Relational mapping algorithm:
 * 
 * Step 1: Map strong entities to tables
 * Step 2: Map weak entities (include owner's PK)
 * Step 3: Map 1:1 relationships (FK in one side)
 * Step 4: Map 1:N relationships (FK in N side)
 * Step 5: Map M:N relationships (junction table)
 * Step 6: Map multi-valued attributes (separate table)
 * Step 7: Map specialization/generalization
 */
public class RelationalMappingGenerator extends DiagramGenerator<RelationalSchema> {

    @Override
    public String getGeneratorName() {
        return "Relational Mapping Generator";
    }

    @Override
    protected RelationalSchema doGenerate(Object input) {
        if (input instanceof ERDiagram erDiagram) {
            return mapToRelational(erDiagram);
        }
        return new RelationalSchema("Empty Schema");
    }

    /**
     * Main mapping algorithm from ER to Relational Schema.
     */
    private RelationalSchema mapToRelational(ERDiagram erDiagram) {
        RelationalSchema schema = new RelationalSchema("Relational Mapping");
        Map<String, RelationalTable> tableMap = new LinkedHashMap<>();

        // Step 1: Map strong entities
        for (EREntity entity : erDiagram.getEntities()) {
            if (!entity.isWeak() && !entity.isSpecializedChild()) {
                RelationalTable table = mapStrongEntity(entity);
                tableMap.put(entity.getName(), table);
                schema.addTable(table);
            }
        }

        // Step 2: Map weak entities
        for (EREntity entity : erDiagram.getEntities()) {
            if (entity.isWeak()) {
                RelationalTable table = mapWeakEntity(entity, erDiagram, tableMap);
                tableMap.put(entity.getName(), table);
                schema.addTable(table);
            }
        }

        // Step 3-4: Map 1:1 and 1:N relationships
        for (ERRelationship rel : erDiagram.getRelationships()) {
            if (rel.isIdentifying()) continue; // Already handled in weak entity mapping

            RelationshipType type = rel.getRelationshipType();

            if (type == RelationshipType.ONE_TO_ONE) {
                mapOneToOne(rel, tableMap);
            } else if (type == RelationshipType.ONE_TO_MANY) {
                mapOneToMany(rel, tableMap);
            }
        }

        // Step 5: Map M:N relationships
        for (ERRelationship rel : erDiagram.getRelationships()) {
            if (rel.getRelationshipType() == RelationshipType.MANY_TO_MANY) {
                RelationalTable junctionTable = mapManyToMany(rel, tableMap);
                tableMap.put(junctionTable.getName(), junctionTable);
                schema.addTable(junctionTable);
            }
        }

        // Step 6: Map multi-valued attributes
        for (EREntity entity : erDiagram.getEntities()) {
            for (ERAttribute attr : entity.getAttributes()) {
                if (attr.isMultiValued()) {
                    RelationalTable mvTable = mapMultiValuedAttribute(entity, attr, tableMap);
                    schema.addTable(mvTable);
                }
            }
        }

        // Step 7: Map specialization
        for (EREntity entity : erDiagram.getEntities()) {
            if (entity.hasSpecialization()) {
                mapSpecialization(entity, erDiagram, tableMap, schema);
            }
        }

        return schema;
    }

    /**
     * Step 1: Map a strong entity to a relational table.
     */
    private RelationalTable mapStrongEntity(EREntity entity) {
        RelationalTable table = new RelationalTable(entity.getName());
        table.setSourceEntityName(entity.getName());

        for (ERAttribute attr : entity.getAttributes()) {
            if (attr.isMultiValued()) continue; // Handled separately in Step 6

            if (attr.isComposite()) {
                // Flatten composite attributes
                for (ERAttribute subAttr : attr.getSubAttributes()) {
                    RelationalColumn col = attributeToColumn(subAttr);
                    table.addColumn(col);
                }
            } else {
                RelationalColumn col = attributeToColumn(attr);
                table.addColumn(col);
            }
        }

        return table;
    }

    /**
     * Step 2: Map a weak entity - include owner's PK as FK+PK.
     */
    private RelationalTable mapWeakEntity(EREntity entity, ERDiagram erDiagram,
                                           Map<String, RelationalTable> tableMap) {
        RelationalTable table = new RelationalTable(entity.getName());
        table.setSourceEntityName(entity.getName());

        // Find the identifying relationship and owner entity
        for (ERRelationship rel : erDiagram.getRelationships()) {
            if (rel.isIdentifying() &&
                    (rel.getEntity1Name().equals(entity.getName())
                            || rel.getEntity2Name().equals(entity.getName()))) {

                String ownerName = rel.getEntity1Name().equals(entity.getName())
                        ? rel.getEntity2Name() : rel.getEntity1Name();

                RelationalTable ownerTable = tableMap.get(ownerName);
                if (ownerTable != null) {
                    // Add owner's PK as FK+PK
                    for (RelationalColumn pk : ownerTable.getPrimaryKeys()) {
                        RelationalColumn fkPk = new RelationalColumn(
                                ownerName.toLowerCase() + "_" + pk.getName(),
                                pk.getDataType(), true);
                        fkPk.setForeignKey(true);
                        fkPk.setReferencedTable(ownerName);
                        fkPk.setReferencedColumn(pk.getName());
                        table.addColumn(fkPk);
                    }
                }
            }
        }

        // Add entity's own attributes
        for (ERAttribute attr : entity.getAttributes()) {
            if (attr.isMultiValued() || attr.isForeignKey()) continue;
            RelationalColumn col = attributeToColumn(attr);
            table.addColumn(col);
        }

        return table;
    }

    /**
     * Step 3: Map 1:1 relationship - add FK to the side with total participation.
     */
    private void mapOneToOne(ERRelationship rel, Map<String, RelationalTable> tableMap) {
        // Add FK to the table with total participation (or entity1 by default)
        String fkTableName = rel.isEntity1TotalParticipation()
                ? rel.getEntity1Name() : rel.getEntity2Name();
        String refTableName = fkTableName.equals(rel.getEntity1Name())
                ? rel.getEntity2Name() : rel.getEntity1Name();

        RelationalTable fkTable = tableMap.get(fkTableName);
        RelationalTable refTable = tableMap.get(refTableName);

        if (fkTable != null && refTable != null) {
            for (RelationalColumn pk : refTable.getPrimaryKeys()) {
                RelationalColumn fk = new RelationalColumn(
                        refTableName.toLowerCase() + "_" + pk.getName(),
                        pk.getDataType());
                fk.setForeignKey(true);
                fk.setUnique(true); // 1:1 means unique FK
                fk.setReferencedTable(refTableName);
                fk.setReferencedColumn(pk.getName());
                fkTable.addColumn(fk);
            }

            // Add relationship attributes to the FK table
            for (ERAttribute attr : rel.getAttributes()) {
                fkTable.addColumn(attributeToColumn(attr));
            }
        }
    }

    /**
     * Step 4: Map 1:N relationship - add FK to the N side.
     */
    private void mapOneToMany(ERRelationship rel, Map<String, RelationalTable> tableMap) {
        // FK goes to the "many" side
        String manySide, oneSide;
        if ("1".equals(rel.getCardinality1())) {
            oneSide = rel.getEntity1Name();
            manySide = rel.getEntity2Name();
        } else {
            oneSide = rel.getEntity2Name();
            manySide = rel.getEntity1Name();
        }

        RelationalTable manyTable = tableMap.get(manySide);
        RelationalTable oneTable = tableMap.get(oneSide);

        if (manyTable != null && oneTable != null) {
            for (RelationalColumn pk : oneTable.getPrimaryKeys()) {
                // Check if this specific FK column already exists
                boolean exists = manyTable.getColumns().stream()
                        .anyMatch(c -> c.isForeignKey()
                                && oneSide.equals(c.getReferencedTable())
                                && pk.getName().equals(c.getReferencedColumn()));
                
                if (!exists) {
                    RelationalColumn fk = new RelationalColumn(
                            oneSide.toLowerCase() + "_" + pk.getName(),
                            pk.getDataType());
                    fk.setForeignKey(true);
                    fk.setReferencedTable(oneSide);
                    fk.setReferencedColumn(pk.getName());
                    manyTable.addColumn(fk);
                }
            }

            // Add relationship attributes to the many table
            for (ERAttribute attr : rel.getAttributes()) {
                manyTable.addColumn(attributeToColumn(attr));
            }
        }
    }

    /**
     * Step 5: Map M:N relationship - create junction/bridge table.
     */
    private RelationalTable mapManyToMany(ERRelationship rel,
                                           Map<String, RelationalTable> tableMap) {
        String tableName = rel.getEntity1Name() + "_" + rel.getEntity2Name();
        RelationalTable junction = new RelationalTable(tableName);
        junction.setSourceEntityName(rel.getName());

        // Add PKs from both entities as composite PK + FK
        RelationalTable table1 = tableMap.get(rel.getEntity1Name());
        RelationalTable table2 = tableMap.get(rel.getEntity2Name());

        if (table1 != null) {
            for (RelationalColumn pk : table1.getPrimaryKeys()) {
                RelationalColumn fkPk = new RelationalColumn(
                        rel.getEntity1Name().toLowerCase() + "_" + pk.getName(),
                        pk.getDataType(), true);
                fkPk.setForeignKey(true);
                fkPk.setReferencedTable(rel.getEntity1Name());
                fkPk.setReferencedColumn(pk.getName());
                junction.addColumn(fkPk);
            }
        }

        if (table2 != null) {
            for (RelationalColumn pk : table2.getPrimaryKeys()) {
                RelationalColumn fkPk = new RelationalColumn(
                        rel.getEntity2Name().toLowerCase() + "_" + pk.getName(),
                        pk.getDataType(), true);
                fkPk.setForeignKey(true);
                fkPk.setReferencedTable(rel.getEntity2Name());
                fkPk.setReferencedColumn(pk.getName());
                junction.addColumn(fkPk);
            }
        }

        // Add relationship attributes
        for (ERAttribute attr : rel.getAttributes()) {
            junction.addColumn(attributeToColumn(attr));
        }

        return junction;
    }

    /**
     * Step 6: Map multi-valued attribute to a separate table.
     */
    private RelationalTable mapMultiValuedAttribute(EREntity entity, ERAttribute attr,
                                                     Map<String, RelationalTable> tableMap) {
        String tableName = entity.getName() + "_" + attr.getName();
        RelationalTable mvTable = new RelationalTable(tableName);

        // Add entity's PK as FK+PK
        RelationalTable entityTable = tableMap.get(entity.getName());
        if (entityTable != null) {
            for (RelationalColumn pk : entityTable.getPrimaryKeys()) {
                RelationalColumn fkPk = new RelationalColumn(
                        entity.getName().toLowerCase() + "_" + pk.getName(),
                        pk.getDataType(), true);
                fkPk.setForeignKey(true);
                fkPk.setReferencedTable(entity.getName());
                fkPk.setReferencedColumn(pk.getName());
                mvTable.addColumn(fkPk);
            }
        }

        // Add the multi-valued attribute as PK
        RelationalColumn valueCol = new RelationalColumn(attr.getName(),
                attr.getDataType() != null ? attr.getDataType() : "VARCHAR(255)", true);
        mvTable.addColumn(valueCol);

        return mvTable;
    }

    /**
     * Step 7: Map specialization/generalization.
     * Uses Option A (table per entity type) approach.
     */
    private void mapSpecialization(EREntity parent, ERDiagram erDiagram,
                                    Map<String, RelationalTable> tableMap,
                                    RelationalSchema schema) {
        for (String childName : parent.getChildEntities()) {
            EREntity child = erDiagram.findEntity(childName).orElse(null);
            if (child == null || tableMap.containsKey(childName)) continue;

            RelationalTable childTable = new RelationalTable(childName);
            childTable.setSourceEntityName(childName);

            // Add parent's PK as PK+FK
            RelationalTable parentTable = tableMap.get(parent.getName());
            if (parentTable != null) {
                for (RelationalColumn pk : parentTable.getPrimaryKeys()) {
                    RelationalColumn fkPk = new RelationalColumn(
                            pk.getName(), pk.getDataType(), true);
                    fkPk.setForeignKey(true);
                    fkPk.setReferencedTable(parent.getName());
                    fkPk.setReferencedColumn(pk.getName());
                    childTable.addColumn(fkPk);
                }
            }

            // Add child's own attributes
            for (ERAttribute attr : child.getAttributes()) {
                if (!attr.isPrimaryKey() && !attr.isForeignKey()) {
                    childTable.addColumn(attributeToColumn(attr));
                }
            }

            tableMap.put(childName, childTable);
            schema.addTable(childTable);
        }
    }

    /**
     * Convert an ERAttribute to a RelationalColumn.
     */
    private RelationalColumn attributeToColumn(ERAttribute attr) {
        RelationalColumn col = new RelationalColumn(attr.getName(),
                attr.getDataType() != null ? attr.getDataType() : "VARCHAR(255)");
        col.setPrimaryKey(attr.isPrimaryKey());
        col.setForeignKey(attr.isForeignKey());
        col.setNotNull(attr.isNotNull() || attr.isPrimaryKey());
        col.setUnique(attr.isUnique());
        col.setDefaultValue(attr.getDefaultValue());
        if (attr.isForeignKey()) {
            col.setReferencedTable(attr.getReferencedTable());
            col.setReferencedColumn(attr.getReferencedColumn());
        }
        return col;
    }
}
