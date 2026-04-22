package com.umlgenerator.core.parser;

import com.umlgenerator.core.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Python source code.
 * Extracts classes, methods, attributes (from __init__), and inheritance.
 * 
 * Handles:
 * - Class definitions with single/multiple inheritance
 * - Instance attributes from __init__ (self.xxx)
 * - Class attributes
 * - Methods with decorators (@abstractmethod, @staticmethod, @classmethod, @property)
 * - Type hints (Python 3.5+)
 * - Access modifiers via naming convention (_ protected, __ private)
 * - Dataclass detection
 */
public class PythonCodeParser implements LanguageParser {

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("^class\\s+(\\w+)(?:\\(([^)]*?)\\))?\\s*:", Pattern.MULTILINE);

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^(\\s+)(?:(@\\w+)\\s+)*def\\s+(\\w+)\\s*\\(([^)]*?)\\)(?:\\s*->\\s*([^:]+))?\\s*:",
                    Pattern.MULTILINE);

    private static final Pattern INIT_ATTR_PATTERN =
            Pattern.compile("self\\.(\\w+)\\s*(?::\\s*([\\w\\[\\],\\s.]+))?\\s*=\\s*(.+?)(?:\\s*#.*)?$",
                    Pattern.MULTILINE);

    private static final Pattern CLASS_ATTR_PATTERN =
            Pattern.compile("^(\\s{4})(\\w+)\\s*(?::\\s*([\\w\\[\\],\\s.]+))?\\s*=\\s*(.+?)$",
                    Pattern.MULTILINE);

    private static final Pattern DECORATOR_PATTERN =
            Pattern.compile("^\\s*@(\\w+)(?:\\(.*\\))?\\s*$", Pattern.MULTILINE);

    private static final Pattern DATACLASS_FIELD_PATTERN =
            Pattern.compile("^\\s{4}(\\w+)\\s*:\\s*([\\w\\[\\],\\s.]+)(?:\\s*=\\s*(.+?))?$",
                    Pattern.MULTILINE);

    @Override
    public String getLanguageName() {
        return "Python";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".py"};
    }

    @Override
    public boolean canParse(String sourceCode) {
        return sourceCode.contains("class ") && sourceCode.contains("def ");
    }

    @Override
    public UMLDiagram parseToUML(String sourceCode) {
        UMLDiagram diagram = new UMLDiagram("Python UML Class Diagram", "Python");

        List<PythonClassBlock> classBlocks = extractClassBlocks(sourceCode);
        Set<String> classNames = new HashSet<>();

        for (PythonClassBlock block : classBlocks) {
            UMLClass umlClass = parseClassBlock(block);
            if (umlClass != null) {
                diagram.addClass(umlClass);
                classNames.add(umlClass.getName());
            }
        }

        // Extract relationships
        extractRelationships(diagram, classNames);

        return diagram;
    }

    private List<PythonClassBlock> extractClassBlocks(String sourceCode) {
        List<PythonClassBlock> blocks = new ArrayList<>();
        Matcher m = CLASS_PATTERN.matcher(sourceCode);

        List<int[]> classPositions = new ArrayList<>();
        while (m.find()) {
            classPositions.add(new int[]{m.start(), m.end()});
        }

        m.reset();
        int index = 0;
        while (m.find()) {
            PythonClassBlock block = new PythonClassBlock();
            block.name = m.group(1);
            block.parentClasses = parseParentClasses(m.group(2));

            // Extract class body (until next class at same indentation level or end)
            int bodyStart = m.end();
            int bodyEnd;
            if (index + 1 < classPositions.size()) {
                bodyEnd = classPositions.get(index + 1)[0];
            } else {
                bodyEnd = sourceCode.length();
            }
            block.body = sourceCode.substring(bodyStart, bodyEnd);
            block.isDataclass = checkDataclass(sourceCode, m.start());
            block.isAbstract = checkAbstractClass(block);

            blocks.add(block);
            index++;
        }

        return blocks;
    }

    private UMLClass parseClassBlock(PythonClassBlock block) {
        UMLClass umlClass = new UMLClass();
        umlClass.setName(block.name);

        // Determine class type
        if (block.isAbstract || block.parentClasses.contains("ABC")
                || block.parentClasses.contains("ABCMeta")) {
            umlClass.setClassType(ClassType.ABSTRACT_CLASS);
        } else {
            umlClass.setClassType(ClassType.CLASS);
        }

        // Inheritance
        List<String> realParents = block.parentClasses.stream()
                .filter(p -> !p.equals("ABC") && !p.equals("ABCMeta")
                        && !p.equals("object") && !p.equals("Enum"))
                .toList();

        if (block.parentClasses.contains("Enum")) {
            umlClass.setClassType(ClassType.ENUM);
        }

        if (!realParents.isEmpty()) {
            umlClass.setParentClass(realParents.get(0));
            if (realParents.size() > 1) {
                umlClass.setInterfaces(realParents.subList(1, realParents.size()));
            }
        }

        // Parse attributes from __init__
        if (block.isDataclass) {
            parseDataclassFields(block.body, umlClass);
        } else {
            parseInitAttributes(block.body, umlClass);
        }

        // Parse class-level attributes
        parseClassAttributes(block.body, umlClass);

        // Parse methods
        parseMethods(block.body, umlClass);

        return umlClass;
    }

    private void parseInitAttributes(String body, UMLClass umlClass) {
        // Find __init__ method body
        Pattern initPattern = Pattern.compile("def\\s+__init__\\s*\\([^)]*\\)\\s*(?:->\\s*None)?\\s*:(.*?)(?=\\n\\s{4}def\\s|$)",
                Pattern.DOTALL);
        Matcher m = initPattern.matcher(body);
        if (m.find()) {
            String initBody = m.group(1);
            Matcher attrMatcher = INIT_ATTR_PATTERN.matcher(initBody);
            Set<String> seen = new HashSet<>();
            while (attrMatcher.find()) {
                String name = attrMatcher.group(1);
                String type = attrMatcher.group(2);
                if (seen.contains(name)) continue;
                seen.add(name);

                if (type == null || type.isEmpty()) type = inferTypeFromValue(attrMatcher.group(3));

                UMLAttribute attr = new UMLAttribute.Builder()
                        .name(name)
                        .type(type)
                        .accessModifier(AccessModifier.fromPythonName(name))
                        .build();
                umlClass.addAttribute(attr);
            }
        }
    }

    private void parseDataclassFields(String body, UMLClass umlClass) {
        Matcher m = DATACLASS_FIELD_PATTERN.matcher(body);
        while (m.find()) {
            String name = m.group(1);
            String type = m.group(2);
            String defaultVal = m.group(3);

            // Skip if it's a method def
            if (name.equals("def") || name.equals("class")) continue;

            UMLAttribute attr = new UMLAttribute.Builder()
                    .name(name)
                    .type(type != null ? type.trim() : "Any")
                    .accessModifier(AccessModifier.fromPythonName(name))
                    .defaultValue(defaultVal)
                    .build();
            umlClass.addAttribute(attr);
        }
    }

    private void parseClassAttributes(String body, UMLClass umlClass) {
        Matcher m = CLASS_ATTR_PATTERN.matcher(body);
        Set<String> existingNames = new HashSet<>();
        umlClass.getAttributes().forEach(a -> existingNames.add(a.getName()));

        while (m.find()) {
            String name = m.group(2);
            String type = m.group(3);
            String value = m.group(4);

            if (existingNames.contains(name) || name.equals("def") || name.equals("class")) continue;

            if (type == null) type = inferTypeFromValue(value);

            UMLAttribute attr = new UMLAttribute.Builder()
                    .name(name)
                    .type(type)
                    .accessModifier(AccessModifier.fromPythonName(name))
                    .isStatic(true) // Class-level attributes are static
                    .defaultValue(value)
                    .build();
            umlClass.addAttribute(attr);
            existingNames.add(name);
        }
    }

    private void parseMethods(String body, UMLClass umlClass) {
        Matcher m = METHOD_PATTERN.matcher(body);
        while (m.find()) {
            String decorator = m.group(2);
            String name = m.group(3);
            String paramsStr = m.group(4);
            String returnType = m.group(5);

            // Skip __init__ as it's handled separately for attributes
            // but include it as a constructor
            boolean isConstructor = name.equals("__init__");

            // Check for decorators by looking backwards from the method
            boolean isAbstract = false;
            boolean isStatic = false;
            boolean isClassMethod = false;
            boolean isProperty = false;

            // Search for decorators above this method
            String beforeMethod = body.substring(0, m.start());
            String[] lines = beforeMethod.split("\n");
            for (int i = lines.length - 1; i >= Math.max(0, lines.length - 5); i--) {
                String line = lines[i].trim();
                if (line.startsWith("@abstractmethod")) isAbstract = true;
                else if (line.startsWith("@staticmethod")) isStatic = true;
                else if (line.startsWith("@classmethod")) isClassMethod = true;
                else if (line.startsWith("@property")) isProperty = true;
                else if (!line.startsWith("@")) break;
            }

            if (decorator != null) {
                if (decorator.contains("abstractmethod")) isAbstract = true;
                if (decorator.contains("staticmethod")) isStatic = true;
                if (decorator.contains("classmethod")) isClassMethod = true;
                if (decorator.contains("property")) isProperty = true;
            }

            // Parse parameters (remove self/cls)
            List<Parameter> params = parsePythonParams(paramsStr);

            UMLMethod method = new UMLMethod.Builder()
                    .name(name)
                    .returnType(returnType != null ? returnType.trim() : "None")
                    .accessModifier(AccessModifier.fromPythonName(name))
                    .isStatic(isStatic || isClassMethod)
                    .isAbstract(isAbstract)
                    .isConstructor(isConstructor)
                    .parameters(params)
                    .build();

            umlClass.addMethod(method);
        }
    }

    private List<Parameter> parsePythonParams(String paramsStr) {
        List<Parameter> params = new ArrayList<>();
        if (paramsStr == null || paramsStr.trim().isEmpty()) return params;

        String[] parts = paramsStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.equals("self") || part.equals("cls") || part.isEmpty()) continue;

            // Check for type hint: name: type = default
            String name, type;
            String defaultVal = null;
            if (part.contains("=")) {
                String[] eqParts = part.split("=", 2);
                part = eqParts[0].trim();
                defaultVal = eqParts[1].trim();
            }
            if (part.contains(":")) {
                String[] typeParts = part.split(":", 2);
                name = typeParts[0].trim();
                type = typeParts[1].trim();
            } else {
                name = part;
                type = "Any";
            }
            params.add(new Parameter(name, type, defaultVal));
        }
        return params;
    }

    private List<String> parseParentClasses(String parentStr) {
        List<String> parents = new ArrayList<>();
        if (parentStr == null || parentStr.trim().isEmpty()) return parents;

        String[] parts = parentStr.split(",");
        for (String part : parts) {
            String p = part.trim().replaceAll("\\[.*\\]", "").trim();
            // Handle metaclass=ABCMeta
            if (p.contains("metaclass=")) {
                p = p.split("=")[1].trim();
            }
            if (!p.isEmpty()) parents.add(p);
        }
        return parents;
    }

    private String inferTypeFromValue(String value) {
        if (value == null) return "Any";
        value = value.trim();
        if (value.startsWith("\"") || value.startsWith("'") || value.startsWith("f\"")) return "str";
        if (value.equals("True") || value.equals("False")) return "bool";
        if (value.equals("None")) return "Optional";
        if (value.startsWith("[")) return "list";
        if (value.startsWith("{")) return "dict";
        if (value.startsWith("(")) return "tuple";
        if (value.matches("\\d+")) return "int";
        if (value.matches("\\d+\\.\\d+")) return "float";
        if (value.endsWith("()")) return value.substring(0, value.length() - 2);
        return "Any";
    }

    private boolean checkDataclass(String sourceCode, int classStart) {
        // Look for @dataclass decorator before class
        int searchStart = Math.max(0, classStart - 200);
        String before = sourceCode.substring(searchStart, classStart);
        return before.contains("@dataclass");
    }

    private boolean checkAbstractClass(PythonClassBlock block) {
        return block.parentClasses.contains("ABC")
                || block.parentClasses.contains("ABCMeta")
                || block.body.contains("@abstractmethod");
    }

    private void extractRelationships(UMLDiagram diagram, Set<String> classNames) {
        for (UMLClass umlClass : diagram.getClasses()) {
            // Inheritance
            if (umlClass.getParentClass() != null && !umlClass.getParentClass().isEmpty()) {
                diagram.addRelationship(new UMLRelationship(
                        umlClass.getName(), umlClass.getParentClass(),
                        RelationshipType.INHERITANCE));
            }

            // Multiple inheritance treated as implementation
            for (String iface : umlClass.getInterfaces()) {
                diagram.addRelationship(new UMLRelationship(
                        umlClass.getName(), iface,
                        RelationshipType.IMPLEMENTATION));
            }

            // Composition from attribute types
            for (UMLAttribute attr : umlClass.getAttributes()) {
                String type = attr.getType();
                if (type != null && classNames.contains(type)) {
                    diagram.addRelationship(new UMLRelationship(
                            umlClass.getName(), type,
                            RelationshipType.COMPOSITION, "1", "1", attr.getName()));
                }
                // Check for List[ClassName] patterns
                if (type != null && (type.startsWith("List[") || type.startsWith("list["))) {
                    String inner = type.substring(type.indexOf('[') + 1, type.length() - 1);
                    if (classNames.contains(inner)) {
                        diagram.addRelationship(new UMLRelationship(
                                umlClass.getName(), inner,
                                RelationshipType.AGGREGATION, "1", "*", attr.getName()));
                    }
                }
            }
        }
    }

    private static class PythonClassBlock {
        String name;
        List<String> parentClasses;
        String body;
        boolean isDataclass;
        boolean isAbstract;
    }
}
