package com.umlgenerator.core.parser;

import com.umlgenerator.core.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SQL DDL (Data Definition Language).
 * Generates both UML class diagrams and ER/EERD diagrams from SQL schemas.
 * 
 * Handles:
 * - CREATE TABLE statements
 * - Column definitions with data types
 * - PRIMARY KEY constraints (inline and table-level)
 * - FOREIGN KEY with REFERENCES
 * - UNIQUE constraints
 * - NOT NULL constraints
 * - DEFAULT values
 * - CHECK constraints (basic)
 * - CREATE TYPE / ENUM types
 * - Composite primary keys
 * - Identifying relationships (FK as part of PK)
 */
public class SQLCodeParser implements LanguageParser {

    private static final Pattern CREATE_TABLE_PATTERN =
            Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`\"']?(\\w+)[`\"']?\\s*\\((.+?)\\)\\s*;",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern COLUMN_PATTERN =
            Pattern.compile("^\\s*[`\"']?(\\w+)[`\"']?\\s+" +
                    "(\\w+(?:\\([^)]*\\))?)\\s*(.*?)$",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern FK_CONSTRAINT_PATTERN =
            Pattern.compile("FOREIGN\\s+KEY\\s*\\([`\"']?(\\w+)[`\"']?\\)\\s+" +
                    "REFERENCES\\s+[`\"']?(\\w+)[`\"']?\\s*\\([`\"']?(\\w+)[`\"']?\\)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PK_CONSTRAINT_PATTERN =
            Pattern.compile("PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern UNIQUE_CONSTRAINT_PATTERN =
            Pattern.compile("UNIQUE\\s*\\(([^)]+)\\)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern INLINE_FK_PATTERN =
            Pattern.compile("REFERENCES\\s+[`\"']?(\\w+)[`\"']?\\s*\\([`\"']?(\\w+)[`\"']?\\)",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public String getLanguageName() {
        return "SQL";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".sql"};
    }

    @Override
    public boolean canParse(String sourceCode) {
        String upper = sourceCode.toUpperCase();
        return upper.contains("CREATE TABLE") || upper.contains("SELECT ") || upper.contains("FROM ");
    }

    @Override
    public boolean supportsERDiagram() {
        return true;
    }

    @Override
    public UMLDiagram parseToUML(String sourceCode) {
        UMLDiagram diagram = new UMLDiagram("SQL Schema UML Diagram", "SQL");

        List<TableDefinition> tables = extractTables(sourceCode);
        Set<String> tableNames = new HashSet<>();

        for (TableDefinition table : tables) {
            UMLClass umlClass = tableToUMLClass(table);
            diagram.addClass(umlClass);
            tableNames.add(table.name);
        }

        // Extract FK relationships
        for (TableDefinition table : tables) {
            for (ForeignKeyDef fk : table.foreignKeys) {
                if (tableNames.contains(fk.referencedTable)) {
                    diagram.addRelationship(new UMLRelationship(
                            table.name, fk.referencedTable,
                            RelationshipType.ASSOCIATION,
                            "*", "1", fk.columnName));
                }
            }
        }

        return diagram;
    }

    @Override
    public ERDiagram parseToER(String sourceCode) {
        ERDiagram erDiagram = new ERDiagram("ER Diagram from SQL Schema");

        List<TableDefinition> tables = extractTables(sourceCode);
        Map<String, EREntity> entityMap = new LinkedHashMap<>();

        // Create entities from tables
        for (TableDefinition table : tables) {
            EREntity entity = tableToEREntity(table);
            erDiagram.addEntity(entity);
            entityMap.put(table.name, entity);
        }

        // Create relationships from foreign keys
        Set<String> processedRels = new HashSet<>();
        for (TableDefinition table : tables) {
            for (ForeignKeyDef fk : table.foreignKeys) {
                String relKey = table.name + "-" + fk.referencedTable;
                String reverseKey = fk.referencedTable + "-" + table.name;

                if (processedRels.contains(relKey) || processedRels.contains(reverseKey)) continue;
                processedRels.add(relKey);

                // Determine cardinality
                boolean fkIsPK = table.primaryKeys.contains(fk.columnName);
                boolean fkIsUnique = table.uniqueColumns.contains(fk.columnName);

                String card1, card2;
                if (fkIsPK && table.primaryKeys.size() > 1) {
                    // Composite PK with FK = M:N junction table
                    card1 = "1..N";
                    card2 = "1..M";
                } else if (fkIsPK || fkIsUnique) {
                    card1 = "1..1";
                    card2 = "0..1";
                } else {
                    card1 = "1..N";
                    card2 = "1..1";
                }

                String relName = fk.columnName.replaceAll("_id$", "")
                        .replaceAll("_", " ");

                ERRelationship rel = new ERRelationship(
                        relName, table.name, fk.referencedTable, card1, card2);

                // Identifying relationship: FK is part of PK
                if (fkIsPK) {
                    rel.setIdentifying(true);
                    EREntity weakEntity = entityMap.get(table.name);
                    if (weakEntity != null && table.primaryKeys.size() > 1) {
                        weakEntity.setWeak(true);
                    }
                }

                // Check NOT NULL for total participation
                if (fk.isNotNull) {
                    rel.setParticipation1("total");
                }

                erDiagram.addRelationship(rel);
            }
        }

        // Detect EERD features
        detectEERDFeatures(erDiagram, tables);

        return erDiagram;
    }

    /**
     * Extract table definitions from SQL source.
     */
    private List<TableDefinition> extractTables(String sourceCode) {
        if (!sourceCode.toUpperCase().contains("CREATE TABLE")) {
            return extractTablesFromQuery(sourceCode);
        }

        List<TableDefinition> tables = new ArrayList<>();
        Matcher m = CREATE_TABLE_PATTERN.matcher(sourceCode);

        while (m.find()) {
            String tableName = m.group(1);
            String body = m.group(2);

            TableDefinition table = new TableDefinition();
            table.name = tableName;

            // Parse columns and constraints
            String[] lines = body.split(",(?![^(]*\\))"); // Split by comma not inside parens

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Check for table-level constraints
                if (line.toUpperCase().startsWith("PRIMARY KEY")) {
                    Matcher pkm = PK_CONSTRAINT_PATTERN.matcher(line);
                    if (pkm.find()) {
                        String[] pkCols = pkm.group(1).split(",");
                        for (String col : pkCols) {
                            table.primaryKeys.add(col.trim().replaceAll("[`\"']", ""));
                        }
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("FOREIGN KEY")) {
                    Matcher fkm = FK_CONSTRAINT_PATTERN.matcher(line);
                    if (fkm.find()) {
                        ForeignKeyDef fk = new ForeignKeyDef();
                        fk.columnName = fkm.group(1);
                        fk.referencedTable = fkm.group(2);
                        fk.referencedColumn = fkm.group(3);
                        table.foreignKeys.add(fk);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("UNIQUE")) {
                    Matcher um = UNIQUE_CONSTRAINT_PATTERN.matcher(line);
                    if (um.find()) {
                        String[] uCols = um.group(1).split(",");
                        for (String col : uCols) {
                            table.uniqueColumns.add(col.trim().replaceAll("[`\"']", ""));
                        }
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("CHECK") || 
                    (line.toUpperCase().startsWith("CONSTRAINT") && line.toUpperCase().contains("CHECK"))) {
                    table.checkConstraints.add(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("INDEX")) {
                    continue;
                }

                // Parse column definition
                Matcher colM = COLUMN_PATTERN.matcher(line);
                if (colM.find()) {
                    ColumnDef col = new ColumnDef();
                    col.name = colM.group(1);
                    col.dataType = colM.group(2);
                    String constraints = colM.group(3) != null ? colM.group(3).toUpperCase() : "";

                    col.isPrimaryKey = constraints.contains("PRIMARY KEY");
                    col.isNotNull = constraints.contains("NOT NULL") || col.isPrimaryKey;
                    col.isUnique = constraints.contains("UNIQUE");

                    if (col.isPrimaryKey) table.primaryKeys.add(col.name);
                    if (col.isUnique) table.uniqueColumns.add(col.name);

                    // Check for inline REFERENCES
                    Matcher inlineFk = INLINE_FK_PATTERN.matcher(constraints);
                    if (inlineFk.find()) {
                        ForeignKeyDef fk = new ForeignKeyDef();
                        fk.columnName = col.name;
                        fk.referencedTable = inlineFk.group(1);
                        fk.referencedColumn = inlineFk.group(2);
                        fk.isNotNull = col.isNotNull;
                        table.foreignKeys.add(fk);
                    }

                    // Extract default value
                    Pattern defaultPattern = Pattern.compile("DEFAULT\\s+(.+?)(?:\\s+|$)",
                            Pattern.CASE_INSENSITIVE);
                    Matcher defM = defaultPattern.matcher(constraints);
                    if (defM.find()) {
                        col.defaultValue = defM.group(1).trim();
                    }

                    table.columns.add(col);
                }
            }

            // Mark FK columns' not-null status
            for (ForeignKeyDef fk : table.foreignKeys) {
                for (ColumnDef col : table.columns) {
                    if (col.name.equals(fk.columnName)) {
                        fk.isNotNull = col.isNotNull;
                    }
                }
            }

            tables.add(table);
        }

        return tables;
    }

    /**
     * Extracts tables and relationships from standard SELECT queries with JOINs.
     */
    private List<TableDefinition> extractTablesFromQuery(String sourceCode) {
        List<TableDefinition> tables = new ArrayList<>();
        Map<String, TableDefinition> aliasMap = new HashMap<>();
        Map<String, TableDefinition> nameMap = new HashMap<>();

        String cleanSource = sourceCode.replaceAll("[\\n\\r]+", " ");
        
        // Match FROM or JOIN followed by table name and optional alias
        Pattern fromJoinPattern = Pattern.compile("(?:FROM|JOIN)\\s+[`\"']?([a-zA-Z0-9_]+)[`\"']?(?:\\s+(?:AS\\s+)?([a-zA-Z0-9_]+))?", Pattern.CASE_INSENSITIVE);
        Matcher m = fromJoinPattern.matcher(cleanSource);
        while (m.find()) {
            String tableName = m.group(1);
            String alias = m.group(2) != null ? m.group(2) : tableName;

            if (isKeyword(tableName) || isKeyword(alias)) continue;

            TableDefinition table = nameMap.computeIfAbsent(tableName, k -> {
                TableDefinition t = new TableDefinition();
                t.name = tableName;
                return t;
            });
            aliasMap.put(alias, table);
        }

        // Add dummy ID column to ensure entity is rendered if no columns parsed
        for (TableDefinition t : nameMap.values()) {
            ColumnDef c = new ColumnDef();
            c.name = "id";
            c.dataType = "INT";
            c.isPrimaryKey = true;
            t.columns.add(c);
            t.primaryKeys.add("id");
        }

        // Parse ON clauses for relationships: ON u.id = o.user_id
        Pattern onPattern = Pattern.compile("ON\\s+([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*=\\s*([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher om = onPattern.matcher(cleanSource);
        while (om.find()) {
            String alias1 = om.group(1);
            String col1 = om.group(2);
            String alias2 = om.group(3);
            String col2 = om.group(4);

            TableDefinition t1 = aliasMap.get(alias1);
            TableDefinition t2 = aliasMap.get(alias2);

            if (t1 != null && t2 != null) {
                // Ensure columns exist
                if (t1.columns.stream().noneMatch(c -> c.name.equals(col1))) {
                    ColumnDef c = new ColumnDef(); c.name = col1; c.dataType = "REF"; t1.columns.add(c);
                }
                if (t2.columns.stream().noneMatch(c -> c.name.equals(col2))) {
                    ColumnDef c = new ColumnDef(); c.name = col2; c.dataType = "REF"; t2.columns.add(c);
                }

                // Create relationship
                if (col1.equalsIgnoreCase("id") || col2.toLowerCase().contains(t1.name.toLowerCase())) {
                    ForeignKeyDef fk = new ForeignKeyDef();
                    fk.columnName = col2;
                    fk.referencedTable = t1.name;
                    fk.referencedColumn = col1;
                    t2.foreignKeys.add(fk);
                } else {
                    ForeignKeyDef fk = new ForeignKeyDef();
                    fk.columnName = col1;
                    fk.referencedTable = t2.name;
                    fk.referencedColumn = col2;
                    t1.foreignKeys.add(fk);
                }
            }
        }

        tables.addAll(nameMap.values());
        return tables;
    }

    private boolean isKeyword(String word) {
        String w = word.toUpperCase();
        return w.equals("INNER") || w.equals("LEFT") || w.equals("RIGHT") || w.equals("OUTER") || 
               w.equals("FULL") || w.equals("JOIN") || w.equals("ON") || w.equals("WHERE") || 
               w.equals("GROUP") || w.equals("ORDER") || w.equals("HAVING") || w.equals("SELECT") || w.equals("AS") || w.equals("AND");
    }

    private UMLClass tableToUMLClass(TableDefinition table) {
        UMLClass umlClass = new UMLClass(table.name, ClassType.CLASS);

        for (ColumnDef col : table.columns) {
            UMLAttribute attr = new UMLAttribute.Builder()
                    .name(col.name)
                    .type(col.dataType)
                    .accessModifier(AccessModifier.PUBLIC)
                    .isFinal(col.isPrimaryKey)
                    .defaultValue(col.defaultValue)
                    .build();
            umlClass.addAttribute(attr);
        }

        return umlClass;
    }

    private EREntity tableToEREntity(TableDefinition table) {
        EREntity entity = new EREntity(table.name);

        Set<String> fkColumns = new HashSet<>();
        for (ForeignKeyDef fk : table.foreignKeys) {
            fkColumns.add(fk.columnName);
        }

        for (ColumnDef col : table.columns) {
            ERAttribute attr = new ERAttribute(col.name, col.dataType);
            attr.setPrimaryKey(table.primaryKeys.contains(col.name));
            attr.setForeignKey(fkColumns.contains(col.name));
            attr.setNotNull(col.isNotNull);
            attr.setUnique(col.isUnique);
            attr.setDefaultValue(col.defaultValue);

            // FK reference info
            if (attr.isForeignKey()) {
                for (ForeignKeyDef fk : table.foreignKeys) {
                    if (fk.columnName.equals(col.name)) {
                        attr.setReferencedTable(fk.referencedTable);
                        attr.setReferencedColumn(fk.referencedColumn);
                    }
                }
            }

            entity.addAttribute(attr);
        }

        // Parse CHECK constraints into business rules
        for (String check : table.checkConstraints) {
            String logic = check.replaceAll("(?i)CHECK\\s*\\(", "").replaceAll("\\)$", "").trim();
            if (logic.toUpperCase().startsWith("CONSTRAINT")) {
                logic = logic.substring(logic.indexOf("CHECK") + 5).trim();
            }
            ERAttribute rule = new ERAttribute("<<Rule>> " + logic, "BUSINESS_RULE");
            entity.addAttribute(rule);
        }

        return entity;
    }

    /**
     * Detect EERD features like specialization/generalization.
     * - Tables sharing the same PK as FK to another table suggest ISA
     * - Type/category columns suggest specialization
     */
    private void detectEERDFeatures(ERDiagram diagram, List<TableDefinition> tables) {
        Map<String, List<String>> parentToChildren = new HashMap<>();

        for (TableDefinition table : tables) {
            // Check if this table's PK is also a FK (suggesting ISA)
            for (ForeignKeyDef fk : table.foreignKeys) {
                if (table.primaryKeys.contains(fk.columnName)
                        && table.primaryKeys.size() == 1) {
                    parentToChildren.computeIfAbsent(fk.referencedTable, k -> new ArrayList<>())
                            .add(table.name);
                }
            }
        }

        // Set specialization relationships
        for (Map.Entry<String, List<String>> entry : parentToChildren.entrySet()) {
            if (entry.getValue().size() >= 2) { // At least 2 children for ISA
                diagram.setEnhanced(true);
                String parentName = entry.getKey();
                EREntity parent = diagram.findEntity(parentName).orElse(null);
                
                if (parent != null) {
                    parent.setChildEntities(entry.getValue());
                    
                    // Disjointness & Completeness Constraint Logic
                    boolean isDisjoint = true;
                    boolean isTotal = false;
                    
                    TableDefinition parentTableDef = tables.stream().filter(t -> t.name.equals(parentName)).findFirst().orElse(null);
                    if (parentTableDef != null) {
                        long typeCols = parentTableDef.columns.stream().filter(c -> c.name.toLowerCase().contains("type") || c.name.toLowerCase().contains("role") || c.name.toLowerCase().contains("kind")).count();
                        long boolCols = parentTableDef.columns.stream().filter(c -> c.name.toLowerCase().startsWith("is_") || c.name.toLowerCase().startsWith("has_")).count();
                        
                        // Overlapping if multiple boolean flags exist
                        if (boolCols >= 2) isDisjoint = false; 
                        
                        // Total specialization if a 'type' column is NOT NULL
                        isTotal = parentTableDef.columns.stream().anyMatch(c -> 
                            (c.name.toLowerCase().contains("type") || c.name.toLowerCase().contains("role")) && c.isNotNull);
                    }
                    
                    parent.setDisjointSpecialization(isDisjoint);
                    parent.setTotalSpecialization(isTotal);
                    
                    for (String childName : entry.getValue()) {
                        EREntity child = diagram.findEntity(childName).orElse(null);
                        if (child != null) {
                            child.setParentEntity(parentName);
                        }
                    }
                }
            }
        }

        // Temporal Logic detection
        for (EREntity entity : diagram.getEntities()) {
            boolean hasHistory = entity.getAttributes().stream().anyMatch(a -> 
                a.getName().toLowerCase().contains("effective_date") ||
                a.getName().toLowerCase().contains("expiry_date") ||
                a.getName().toLowerCase().contains("valid_from") ||
                a.getName().toLowerCase().contains("valid_to") ||
                a.getName().toLowerCase().contains("history")
            );
            if (hasHistory) {
                entity.setName(entity.getName() + " <<Temporal>>");
            }
        }

        // Aggregation Logic detection (Relationship between entity and another relationship)
        for (EREntity entity : diagram.getEntities()) {
            // A junction table (composite PK of FKs) that is referenced by another table is an Aggregate
            boolean isJunction = entity.getPrimaryKeys().size() >= 2 && 
                                 entity.getPrimaryKeys().stream().allMatch(ERAttribute::isForeignKey);
            
            if (isJunction) {
                boolean isReferenced = tables.stream().anyMatch(t -> 
                    t.foreignKeys.stream().anyMatch(fk -> fk.referencedTable.equals(entity.getName()))
                );
                
                if (isReferenced) {
                    entity.setName(entity.getName() + " <<Aggregate>>");
                }
            }
        }
    }

    // ---- Internal data classes ----

    private static class TableDefinition {
        String name;
        List<ColumnDef> columns = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<String> uniqueColumns = new ArrayList<>();
        List<ForeignKeyDef> foreignKeys = new ArrayList<>();
        List<String> checkConstraints = new ArrayList<>();
    }

    private static class ColumnDef {
        String name;
        String dataType;
        boolean isPrimaryKey;
        boolean isNotNull;
        boolean isUnique;
        String defaultValue;
    }

    private static class ForeignKeyDef {
        String columnName;
        String referencedTable;
        String referencedColumn;
        boolean isNotNull;
    }
}
