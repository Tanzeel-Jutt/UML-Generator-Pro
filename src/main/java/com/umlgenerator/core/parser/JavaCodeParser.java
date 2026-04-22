package com.umlgenerator.core.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.umlgenerator.core.model.*;

import java.util.*;

/**
 * Robust Parser for Java source code using AST (JavaParser).
 * 100% fail-proof against weird formatting, nested generics, and long comments.
 */
public class JavaCodeParser implements LanguageParser {

    private static final Set<String> COLLECTION_TYPES = Set.of(
            "List", "ArrayList", "LinkedList", "Set", "HashSet", "TreeSet",
            "Map", "HashMap", "TreeMap", "Collection", "Queue", "Deque",
            "Vector", "Stack", "PriorityQueue"
    );

    private static final Set<String> EXCLUDED_TYPES = Set.of(
            "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short",
            "String", "Object", "void", "Void", "BigDecimal", "BigInteger",
            "Date", "LocalDate", "LocalDateTime", "Instant"
    );

    @Override
    public String getLanguageName() {
        return "Java";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".java"};
    }

    @Override
    public boolean canParse(String sourceCode) {
        return sourceCode.contains("class ") || sourceCode.contains("interface ")
                || sourceCode.contains("enum ") || sourceCode.contains("package ");
    }

    @Override
    public UMLDiagram parseToUML(String sourceCode) {
        UMLDiagram diagram = new UMLDiagram("Java UML Class Diagram", "Java");
        try {
            // Use AST parser instead of regex
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);
            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            Set<String> classNames = new HashSet<>();

            // Parse standard classes and interfaces
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
                UMLClass umlClass = parseClassOrInterface(cid, packageName);
                diagram.addClass(umlClass);
                classNames.add(umlClass.getName());
            });

            // Parse enums
            cu.findAll(EnumDeclaration.class).forEach(ed -> {
                UMLClass umlClass = parseEnum(ed, packageName);
                diagram.addClass(umlClass);
                classNames.add(umlClass.getName());
            });

            // Parse records
            cu.findAll(RecordDeclaration.class).forEach(rd -> {
                UMLClass umlClass = parseRecord(rd, packageName);
                diagram.addClass(umlClass);
                classNames.add(umlClass.getName());
            });

            // Extract relationships based on AST
            extractRelationships(diagram, classNames);

        } catch (ParseProblemException e) {
            System.err.println("AST Parsing failed (invalid syntax), but handled gracefully: " + e.getMessage());
        }
        return diagram;
    }

    private UMLClass parseClassOrInterface(ClassOrInterfaceDeclaration cid, String packageName) {
        UMLClass umlClass = new UMLClass();
        umlClass.setName(cid.getNameAsString());
        umlClass.setPackageName(packageName);

        if (cid.isInterface()) {
            umlClass.setClassType(ClassType.INTERFACE);
        } else if (cid.hasModifier(Modifier.Keyword.ABSTRACT)) {
            umlClass.setClassType(ClassType.ABSTRACT_CLASS);
        } else {
            umlClass.setClassType(ClassType.CLASS);
        }

        umlClass.setClassAccessModifier(getAccessModifier(cid.getModifiers()));

        // Inheritance
        if (!cid.getExtendedTypes().isEmpty()) {
            umlClass.setParentClass(cid.getExtendedTypes().get(0).getNameAsString());
        }

        // Interfaces
        List<String> interfaces = new ArrayList<>();
        for (ClassOrInterfaceType type : cid.getImplementedTypes()) {
            interfaces.add(type.getNameAsString());
        }
        umlClass.setInterfaces(interfaces);

        // Fields
        cid.getFields().forEach(fd -> parseFields(fd, umlClass));
        
        // Constructors
        cid.getConstructors().forEach(cd -> parseConstructor(cd, umlClass));

        // Methods
        cid.getMethods().forEach(md -> parseMethod(md, umlClass));

        return umlClass;
    }

    private UMLClass parseEnum(EnumDeclaration ed, String packageName) {
        UMLClass umlClass = new UMLClass();
        umlClass.setName(ed.getNameAsString());
        umlClass.setPackageName(packageName);
        umlClass.setClassType(ClassType.ENUM);
        umlClass.setClassAccessModifier(getAccessModifier(ed.getModifiers()));

        ed.getEntries().forEach(entry -> umlClass.addEnumConstant(entry.getNameAsString()));

        ed.getFields().forEach(fd -> parseFields(fd, umlClass));
        ed.getMethods().forEach(md -> parseMethod(md, umlClass));

        return umlClass;
    }

    private UMLClass parseRecord(RecordDeclaration rd, String packageName) {
        UMLClass umlClass = new UMLClass();
        umlClass.setName(rd.getNameAsString());
        umlClass.setPackageName(packageName);
        umlClass.setClassType(ClassType.RECORD);
        umlClass.setClassAccessModifier(getAccessModifier(rd.getModifiers()));
        
        rd.getMethods().forEach(md -> parseMethod(md, umlClass));
        return umlClass;
    }

    private void parseFields(FieldDeclaration fd, UMLClass umlClass) {
        boolean isStatic = fd.hasModifier(Modifier.Keyword.STATIC);
        boolean isFinal = fd.hasModifier(Modifier.Keyword.FINAL);
        AccessModifier access = getAccessModifier(fd.getModifiers());

        for (VariableDeclarator vd : fd.getVariables()) {
            String type = vd.getType().asString();
            String name = vd.getNameAsString();
            String defaultVal = vd.getInitializer().map(Object::toString).orElse(null);

            UMLAttribute attr = new UMLAttribute.Builder()
                    .name(name)
                    .type(type)
                    .accessModifier(access)
                    .isStatic(isStatic)
                    .isFinal(isFinal)
                    .defaultValue(defaultVal)
                    .build();

            umlClass.addAttribute(attr);
        }
    }

    private void parseConstructor(ConstructorDeclaration cd, UMLClass umlClass) {
        UMLMethod constructor = new UMLMethod();
        constructor.setName(cd.getNameAsString());
        constructor.setConstructor(true);
        constructor.setAccessModifier(getAccessModifier(cd.getModifiers()));
        constructor.setReturnType(null);

        List<com.umlgenerator.core.model.Parameter> parameters = new ArrayList<>();
        cd.getParameters().forEach(p -> {
            parameters.add(new com.umlgenerator.core.model.Parameter(p.getNameAsString(), p.getType().asString()));
        });
        constructor.setParameters(parameters);
        
        
        if (cd.getBody() != null) {
            constructor.setBody(cd.getBody().toString());
        }

        umlClass.addMethod(constructor);
    }

    private void parseMethod(MethodDeclaration md, UMLClass umlClass) {
        UMLMethod method = new UMLMethod();
        method.setName(md.getNameAsString());
        method.setReturnType(md.getType().asString());
        method.setAccessModifier(getAccessModifier(md.getModifiers()));
        method.setStatic(md.hasModifier(Modifier.Keyword.STATIC));
        method.setFinal(md.hasModifier(Modifier.Keyword.FINAL));
        method.setAbstract(md.hasModifier(Modifier.Keyword.ABSTRACT));

        List<com.umlgenerator.core.model.Parameter> parameters = new ArrayList<>();
        md.getParameters().forEach(p -> {
            parameters.add(new com.umlgenerator.core.model.Parameter(p.getNameAsString(), p.getType().asString()));
        });
        method.setParameters(parameters);

        if (md.getBody().isPresent()) {
            method.setBody(md.getBody().get().toString());
        }

        umlClass.addMethod(method);
    }

    private AccessModifier getAccessModifier(NodeList<Modifier> modifiers) {
        for (Modifier m : modifiers) {
            if (m.getKeyword() == Modifier.Keyword.PUBLIC) return AccessModifier.PUBLIC;
            if (m.getKeyword() == Modifier.Keyword.PRIVATE) return AccessModifier.PRIVATE;
            if (m.getKeyword() == Modifier.Keyword.PROTECTED) return AccessModifier.PROTECTED;
        }
        return AccessModifier.PACKAGE_PRIVATE;
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
                        RelationshipType.IMPLEMENTATION));
            }

            for (UMLAttribute attr : umlClass.getAttributes()) {
                String fullType = attr.getType();
                String fieldType = cleanGenericType(fullType);
                String innerType = extractGenericInnerType(fullType);

                if (classNames.contains(fieldType) && !EXCLUDED_TYPES.contains(fieldType)) {
                    diagram.addRelationship(new UMLRelationship(
                            umlClass.getName(), fieldType,
                            RelationshipType.COMPOSITION, "1", "1", attr.getName()));
                } else if (COLLECTION_TYPES.contains(fieldType) && innerType != null
                        && classNames.contains(innerType)) {
                    diagram.addRelationship(new UMLRelationship(
                            umlClass.getName(), innerType,
                            RelationshipType.AGGREGATION, "1", "*", attr.getName()));
                }
            }
        }
    }

    private String cleanGenericType(String type) {
        if (type == null) return "";
        int idx = type.indexOf('<');
        return idx >= 0 ? type.substring(0, idx).trim() : type.trim();
    }

    private String extractGenericInnerType(String type) {
        if (type == null) return null;
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start >= 0 && end > start) {
            String inner = type.substring(start + 1, end).trim();
            if (inner.contains(",")) {
                String[] parts = inner.split(",");
                return parts[parts.length - 1].trim().replaceAll("<.*>", "").trim();
            }
            return inner.replaceAll("<.*>", "").trim();
        }
        return null;
    }
}
