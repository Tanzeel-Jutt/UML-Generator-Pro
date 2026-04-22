package com.umlgenerator.core.parser;

import com.umlgenerator.core.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for C++ source code.
 * Extracts classes, structs, methods, fields, and inheritance.
 * 
 * Handles:
 * - Class/Struct declarations with access sections (public/private/protected)
 * - Virtual and pure virtual methods (abstract)
 * - Static members
 * - Const methods
 * - Constructor/Destructor detection
 * - Single and multiple inheritance
 * - Template classes (basic support)
 */
public class CppCodeParser implements LanguageParser {

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("(?:template\\s*<[^>]+>\\s*)?" +
                    "(class|struct)\\s+(\\w+)(?:\\s*:\\s*(.+?))?\\s*\\{",
                    Pattern.MULTILINE);

    private static final Pattern FIELD_PATTERN =
            Pattern.compile("^\\s*(static\\s+)?(const\\s+)?(volatile\\s+)?" +
                    "([\\w:<>*&\\s]+?)\\s+(\\w+)(?:\\s*=\\s*(.+?))?\\s*;",
                    Pattern.MULTILINE);

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*(virtual\\s+)?(static\\s+)?(explicit\\s+)?" +
                    "(?:inline\\s+)?(const\\s+)?" +
                    "([\\w:<>*&]+(?:\\s+[\\w:<>*&]+)*)\\s+" +
                    "(\\w+)\\s*\\(([^)]*)\\)" +
                    "(?:\\s*const)?(?:\\s*override)?(?:\\s*=\\s*0)?(?:\\s*noexcept)?" +
                    "\\s*[{;]",
                    Pattern.MULTILINE);

    private static final Pattern PURE_VIRTUAL_PATTERN =
            Pattern.compile("=\\s*0\\s*;");

    private static final Pattern ACCESS_SECTION_PATTERN =
            Pattern.compile("^\\s*(public|private|protected)\\s*:", Pattern.MULTILINE);

    @Override
    public String getLanguageName() {
        return "C++";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".cpp", ".h", ".hpp", ".cc", ".cxx"};
    }

    @Override
    public boolean canParse(String sourceCode) {
        return (sourceCode.contains("class ") || sourceCode.contains("struct "))
                && (sourceCode.contains("public:") || sourceCode.contains("private:")
                || sourceCode.contains("protected:") || sourceCode.contains("#include")
                || sourceCode.contains("virtual") || sourceCode.contains("::"));
    }

    @Override
    public UMLDiagram parseToUML(String sourceCode) {
        UMLDiagram diagram = new UMLDiagram("C++ UML Class Diagram", "C++");

        List<CppClassBlock> classBlocks = extractClassBlocks(sourceCode);
        Set<String> classNames = new HashSet<>();

        for (CppClassBlock block : classBlocks) {
            UMLClass umlClass = parseClassBlock(block);
            if (umlClass != null) {
                diagram.addClass(umlClass);
                classNames.add(umlClass.getName());
            }
        }

        extractRelationships(diagram, classNames);
        return diagram;
    }

    private List<CppClassBlock> extractClassBlocks(String sourceCode) {
        List<CppClassBlock> blocks = new ArrayList<>();
        Matcher m = CLASS_PATTERN.matcher(sourceCode);

        while (m.find()) {
            CppClassBlock block = new CppClassBlock();
            block.isStruct = m.group(1).equals("struct");
            block.name = m.group(2);
            block.inheritance = m.group(3);

            // Find matching closing brace
            int bodyStart = m.end();
            int bodyEnd = findMatchingBrace(sourceCode, bodyStart - 1);
            block.body = (bodyEnd > bodyStart) ? sourceCode.substring(bodyStart, bodyEnd) : "";

            blocks.add(block);
        }

        return blocks;
    }

    private UMLClass parseClassBlock(CppClassBlock block) {
        UMLClass umlClass = new UMLClass();
        umlClass.setName(block.name);

        // Parse inheritance
        boolean hasAbstractMethods = block.body.contains("= 0;");
        if (hasAbstractMethods) {
            umlClass.setClassType(ClassType.ABSTRACT_CLASS);
        } else {
            umlClass.setClassType(ClassType.CLASS);
        }

        // Parse inheritance list
        if (block.inheritance != null) {
            parseInheritance(block.inheritance, umlClass);
        }

        // Split body into access sections
        parseMembersWithAccessSections(block, umlClass);

        return umlClass;
    }

    private void parseInheritance(String inheritanceStr, UMLClass umlClass) {
        String[] parts = inheritanceStr.split(",");
        boolean first = true;
        for (String part : parts) {
            part = part.trim();
            // Remove access specifier
            part = part.replaceAll("\\b(public|private|protected)\\b", "").trim();
            if (!part.isEmpty()) {
                if (first) {
                    umlClass.setParentClass(part);
                    first = false;
                } else {
                    umlClass.addInterface(part);
                }
            }
        }
    }

    private void parseMembersWithAccessSections(CppClassBlock block, UMLClass umlClass) {
        String body = block.body;

        // Default access: private for class, public for struct
        AccessModifier currentAccess = block.isStruct ?
                AccessModifier.PUBLIC : AccessModifier.PRIVATE;

        // Split by access sections
        Matcher accessMatcher = ACCESS_SECTION_PATTERN.matcher(body);
        List<AccessSection> sections = new ArrayList<>();
        int lastEnd = 0;

        // Add initial section with default access
        while (accessMatcher.find()) {
            if (lastEnd < accessMatcher.start()) {
                sections.add(new AccessSection(currentAccess,
                        body.substring(lastEnd, accessMatcher.start())));
            }
            currentAccess = AccessModifier.fromKeyword(accessMatcher.group(1));
            lastEnd = accessMatcher.end();
        }
        // Add remaining section
        if (lastEnd < body.length()) {
            sections.add(new AccessSection(currentAccess,
                    body.substring(lastEnd)));
        }

        // Parse each section
        for (AccessSection section : sections) {
            parseFieldsInSection(section.content, section.access, umlClass, block.name);
            parseMethodsInSection(section.content, section.access, umlClass, block.name);
        }
    }

    private void parseFieldsInSection(String content, AccessModifier access,
                                       UMLClass umlClass, String className) {
        Matcher m = FIELD_PATTERN.matcher(content);
        while (m.find()) {
            boolean isStatic = m.group(1) != null;
            boolean isConst = m.group(2) != null;
            String type = m.group(4) != null ? m.group(4).trim() : "auto";
            String name = m.group(5);
            String defaultVal = m.group(6);

            // Skip things that look like methods or keywords
            if (name.equals(className) || type.contains("(") || type.contains(")")
                    || type.equals("return") || type.equals("if") || type.equals("for")) {
                continue;
            }

            UMLAttribute attr = new UMLAttribute.Builder()
                    .name(name)
                    .type(isConst ? "const " + type : type)
                    .accessModifier(access)
                    .isStatic(isStatic)
                    .isFinal(isConst)
                    .defaultValue(defaultVal != null ? defaultVal.trim() : null)
                    .build();

            umlClass.addAttribute(attr);
        }
    }

    private void parseMethodsInSection(String content, AccessModifier access,
                                        UMLClass umlClass, String className) {
        Matcher m = METHOD_PATTERN.matcher(content);
        while (m.find()) {
            boolean isVirtual = m.group(1) != null;
            boolean isStatic = m.group(2) != null;
            String returnType = m.group(5) != null ? m.group(5).trim() : "void";
            String name = m.group(6);
            String paramsStr = m.group(7);

            boolean isConstructor = name.equals(className);
            boolean isDestructor = name.equals("~" + className) || returnType.contains("~");

            // Check for pure virtual
            String fullMatch = m.group(0);
            boolean isPureVirtual = PURE_VIRTUAL_PATTERN.matcher(fullMatch).find();

            if (isDestructor) {
                name = "~" + className;
                returnType = "void";
            }

            UMLMethod method = new UMLMethod.Builder()
                    .name(name)
                    .returnType(isConstructor ? null : returnType)
                    .accessModifier(access)
                    .isStatic(isStatic)
                    .isAbstract(isPureVirtual)
                    .isConstructor(isConstructor)
                    .parameters(parseCppParams(paramsStr))
                    .build();

            umlClass.addMethod(method);
        }
    }

    private List<Parameter> parseCppParams(String paramsStr) {
        List<Parameter> params = new ArrayList<>();
        if (paramsStr == null || paramsStr.trim().isEmpty()) return params;

        String[] parts = paramsStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // Remove default values
            String defaultVal = null;
            if (part.contains("=")) {
                String[] eqParts = part.split("=", 2);
                part = eqParts[0].trim();
                defaultVal = eqParts[1].trim();
            }

            // Split "const string& name" -> type="const string&", name="name"
            int lastSpace = part.lastIndexOf(' ');
            if (lastSpace > 0) {
                String type = part.substring(0, lastSpace).trim();
                String name = part.substring(lastSpace + 1).trim()
                        .replaceAll("[&*]", ""); // Remove ref/ptr from name
                params.add(new Parameter(name, type, defaultVal));
            }
        }
        return params;
    }

    private void extractRelationships(UMLDiagram diagram, Set<String> classNames) {
        for (UMLClass umlClass : diagram.getClasses()) {
            if (umlClass.getParentClass() != null && !umlClass.getParentClass().isEmpty()) {
                diagram.addRelationship(new UMLRelationship(
                        umlClass.getName(), umlClass.getParentClass(),
                        RelationshipType.INHERITANCE));
            }

            for (String iface : umlClass.getInterfaces()) {
                diagram.addRelationship(new UMLRelationship(
                        umlClass.getName(), iface,
                        RelationshipType.INHERITANCE)); // C++ uses inheritance for interfaces too
            }

            // Composition from member types
            for (UMLAttribute attr : umlClass.getAttributes()) {
                String type = attr.getType().replaceAll("[*&]", "")
                        .replaceAll("const\\s+", "").replaceAll("\\s+", "").trim();
                if (classNames.contains(type)) {
                    boolean isPointer = attr.getType().contains("*");
                    diagram.addRelationship(new UMLRelationship(
                            umlClass.getName(), type,
                            isPointer ? RelationshipType.AGGREGATION : RelationshipType.COMPOSITION,
                            "1", "1", attr.getName()));
                }
                // Check for vector<ClassName>, list<ClassName>, etc.
                if (type.contains("vector<") || type.contains("list<") || type.contains("set<")) {
                    String inner = type.replaceAll(".*<(\\w+)>.*", "$1");
                    if (classNames.contains(inner)) {
                        diagram.addRelationship(new UMLRelationship(
                                umlClass.getName(), inner,
                                RelationshipType.AGGREGATION, "1", "*", attr.getName()));
                    }
                }
            }
        }
    }

    private int findMatchingBrace(String source, int openPos) {
        int depth = 0;
        for (int i = openPos; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return source.length();
    }

    private static class CppClassBlock {
        boolean isStruct;
        String name;
        String inheritance;
        String body;
    }

    private static class AccessSection {
        AccessModifier access;
        String content;

        AccessSection(AccessModifier access, String content) {
            this.access = access;
            this.content = content;
        }
    }
}
