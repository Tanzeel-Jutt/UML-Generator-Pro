package com.umlgenerator.core.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates boilerplate source code from UML text notation.
 * 
 * Supports parsing UML-style class definitions:
 *   Class name box:    ClassName  or  <<abstract>> ClassName  or  <<interface>> ClassName
 *   Attributes box:    + name: String
 *                      - age: int
 *                      # count: int {static}
 *                      - MAX: int {static, final}
 *   Methods box:       + getName(): String
 *                      + setName(name: String): void
 *                      + ClassName(param: Type)          ← constructor (same name as class)
 *                      # calculate(x: int, y: int): double {static}
 *                      + doSomething(): void {abstract}
 * 
 * Access Modifiers:  + public, - private, # protected, ~ package-private
 * Modifiers:         {static}  {final}  {abstract}  {static, final}
 */
public class BoilerplateCodeGenerator {

    // ──────────────── INNER DATA CLASSES ────────────────

    public static class ParsedClass {
        public String name = "";
        public String stereotype = "";          // "abstract", "interface", "enum", ""
        public List<ParsedAttribute> attributes = new ArrayList<>();
        public List<ParsedMethod> methods = new ArrayList<>();
    }

    public static class ParsedAttribute {
        public String access = "private";       // public, private, protected, package
        public String name = "";
        public String type = "Object";
        public boolean isStatic = false;
        public boolean isFinal = false;
        public String defaultValue = null;
    }

    public static class ParsedMethod {
        public String access = "public";
        public String name = "";
        public String returnType = "void";
        public boolean isConstructor = false;
        public boolean isStatic = false;
        public boolean isAbstract = false;
        public String body = "";
        public List<String[]> params = new ArrayList<>(); // [name, type] pairs
    }

    // ──────────────── PARSING ────────────────

    public ParsedClass parse(String classBox, String attrBox, String methodBox) {
        ParsedClass pc = new ParsedClass();
        parseClassName(classBox.trim(), pc);
        parseAttributes(attrBox.trim(), pc);
        parseMethods(methodBox.trim(), pc);
        return pc;
    }

    private void parseClassName(String text, ParsedClass pc) {
        if (text.isEmpty()) return;
        // Check for stereotype markers
        String lower = text.toLowerCase();
        if (lower.contains("<<abstract>>") || lower.contains("«abstract»")) {
            pc.stereotype = "abstract";
            text = text.replaceAll("(?i)<<abstract>>|«abstract»", "").trim();
        } else if (lower.contains("<<interface>>") || lower.contains("«interface»")) {
            pc.stereotype = "interface";
            text = text.replaceAll("(?i)<<interface>>|«interface»", "").trim();
        } else if (lower.contains("<<enum>>") || lower.contains("«enum»") 
                || lower.contains("<<enumeration>>") || lower.contains("«enumeration»")) {
            pc.stereotype = "enum";
            text = text.replaceAll("(?i)<<enum(eration)?>>|«enum(eration)?»", "").trim();
        }
        pc.name = text.split("\\s+")[0]; // first word
    }

    private void parseAttributes(String text, ParsedClass pc) {
        if (text.isEmpty()) return;
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            ParsedAttribute attr = new ParsedAttribute();

            // Extract modifiers from {…}
            String modStr = extractModifiers(line);
            line = removeModifiers(line);
            attr.isStatic = modStr.contains("static");
            attr.isFinal = modStr.contains("final");

            // Access modifier
            attr.access = parseAccess(line);
            line = stripAccessSymbol(line);

            // name: Type = default
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                attr.name = parts[0].trim();
                String rest = parts[1].trim();
                if (rest.contains("=")) {
                    String[] vp = rest.split("=", 2);
                    attr.type = vp[0].trim();
                    attr.defaultValue = vp[1].trim();
                } else {
                    attr.type = rest;
                }
            } else {
                attr.name = line.trim();
            }
            if (!attr.name.isEmpty()) pc.attributes.add(attr);
        }
    }

    private void parseMethods(String text, ParsedClass pc) {
        if (text.isEmpty()) return;
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            ParsedMethod m = new ParsedMethod();

            // Extract modifiers from {…}
            String modStr = extractModifiers(line);
            line = removeModifiers(line);
            m.isStatic = modStr.contains("static");
            m.isAbstract = modStr.contains("abstract");

            // Access modifier
            m.access = parseAccess(line);
            line = stripAccessSymbol(line);

            // name(params): ReturnType
            int parenOpen = line.indexOf('(');
            int parenClose = line.lastIndexOf(')');
            if (parenOpen >= 0 && parenClose > parenOpen) {
                m.name = line.substring(0, parenOpen).trim();
                String paramStr = line.substring(parenOpen + 1, parenClose).trim();
                if (!paramStr.isEmpty()) {
                    for (String p : paramStr.split(",")) {
                        p = p.trim();
                        if (p.contains(":")) {
                            String[] pp = p.split(":", 2);
                            m.params.add(new String[]{pp[0].trim(), pp[1].trim()});
                        } else {
                            m.params.add(new String[]{p, "Object"});
                        }
                    }
                }
                String afterParen = line.substring(parenClose + 1).trim();
                if (afterParen.startsWith(":")) {
                    m.returnType = afterParen.substring(1).trim();
                }
            } else {
                m.name = line;
            }

            // Constructor detection
            if (m.name.equals(pc.name)) {
                m.isConstructor = true;
                m.returnType = "";
            }

            if (!m.name.isEmpty()) pc.methods.add(m);
        }
    }

    // ──────────────── CODE GENERATION ────────────────

    public String generateCode(ParsedClass pc, String language) {
        return switch (language.toLowerCase()) {
            case "java" -> generateJava(pc);
            case "python" -> generatePython(pc);
            case "c++" -> generateCpp(pc);
            default -> generateJava(pc);
        };
    }

    // ════════════════ JAVA ════════════════
    private String generateJava(ParsedClass pc) {
        StringBuilder sb = new StringBuilder();
        boolean isInterface = "interface".equals(pc.stereotype);
        boolean isAbstract = "abstract".equals(pc.stereotype);
        boolean isEnum = "enum".equals(pc.stereotype);

        // Class declaration
        sb.append("public ");
        if (isAbstract) sb.append("abstract ");
        if (isInterface) sb.append("interface ");
        else if (isEnum) sb.append("enum ");
        else sb.append("class ");
        sb.append(pc.name).append(" {\n\n");

        // Attributes
        for (ParsedAttribute a : pc.attributes) {
            sb.append("    ");
            if (!a.access.isEmpty()) sb.append(a.access).append(" ");
            if (a.isStatic) sb.append("static ");
            if (a.isFinal) sb.append("final ");
            sb.append(a.type).append(" ").append(a.name);
            if (a.defaultValue != null) sb.append(" = ").append(a.defaultValue);
            sb.append(";\n");
        }
        if (!pc.attributes.isEmpty()) sb.append("\n");

        // Default constructor (always generate for class/abstract unless user already wrote a no-arg ctor)
        boolean hasUserDefaultCtor = pc.methods.stream()
                .anyMatch(m -> m.isConstructor && m.params.isEmpty());
        if (!isInterface && !isEnum && !hasUserDefaultCtor) {
            sb.append("    public ").append(pc.name).append("() {\n");
            sb.append("        // Default constructor\n");
            sb.append("    }\n\n");
        }

        // Methods
        for (ParsedMethod m : pc.methods) {
            sb.append("    ");
            if (!m.access.isEmpty()) sb.append(m.access).append(" ");
            if (m.isStatic) sb.append("static ");
            if (m.isAbstract && !isInterface) sb.append("abstract ");

            if (m.isConstructor) {
                sb.append(pc.name).append("(");
            } else {
                String rt = m.returnType.isEmpty() ? "void" : m.returnType;
                sb.append(rt).append(" ").append(m.name).append("(");
            }
            appendJavaParams(sb, m.params);
            sb.append(")");

            if (m.isAbstract || isInterface) {
                sb.append(";\n\n");
            } else {
                sb.append(" {\n");
                if (m.isConstructor) {
                    for (String[] p : m.params) {
                        sb.append("        this.").append(p[0]).append(" = ").append(p[0]).append(";\n");
                    }
                } else {
                    String rt = m.returnType.isEmpty() ? "void" : m.returnType;
                    appendJavaReturn(sb, rt);
                }
                sb.append("    }\n\n");
            }
        }

        // Getters & setters for non-interface non-enum
        if (!isInterface && !isEnum) {
            for (ParsedAttribute a : pc.attributes) {
                if (a.isStatic && a.isFinal) continue; // skip constants
                // Getter
                String cap = capitalize(a.name);
                String prefix = "boolean".equalsIgnoreCase(a.type) ? "is" : "get";
                sb.append("    public ").append(a.type).append(" ")
                  .append(prefix).append(cap).append("() {\n");
                sb.append("        return this.").append(a.name).append(";\n");
                sb.append("    }\n\n");
                // Setter
                if (!a.isFinal) {
                    sb.append("    public void set").append(cap)
                      .append("(").append(a.type).append(" ").append(a.name).append(") {\n");
                    sb.append("        this.").append(a.name).append(" = ").append(a.name).append(";\n");
                    sb.append("    }\n\n");
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void appendJavaParams(StringBuilder sb, List<String[]> params) {
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i)[1]).append(" ").append(params.get(i)[0]);
        }
    }

    private void appendJavaReturn(StringBuilder sb, String type) {
        switch (type.toLowerCase()) {
            case "void" -> sb.append("        // TODO: implement\n");
            case "int", "long", "short", "byte" -> sb.append("        return 0;\n");
            case "double", "float" -> sb.append("        return 0.0;\n");
            case "boolean" -> sb.append("        return false;\n");
            case "char" -> sb.append("        return '\\0';\n");
            case "string" -> sb.append("        return \"\";\n");
            default -> sb.append("        return null;\n");
        }
    }

    // ════════════════ PYTHON ════════════════
    private String generatePython(ParsedClass pc) {
        StringBuilder sb = new StringBuilder();
        boolean isAbstract = "abstract".equals(pc.stereotype);

        if (isAbstract) {
            sb.append("from abc import ABC, abstractmethod\n\n");
        }

        sb.append("class ").append(pc.name);
        if (isAbstract) sb.append("(ABC)");
        sb.append(":\n");

        // Class-level (static) attributes
        boolean hasClassAttrs = false;
        for (ParsedAttribute a : pc.attributes) {
            if (a.isStatic) {
                sb.append("    ").append(pyAttrName(a)).append(" = ");
                sb.append(a.defaultValue != null ? a.defaultValue : pyDefault(a.type));
                sb.append("\n");
                hasClassAttrs = true;
            }
        }
        if (hasClassAttrs) sb.append("\n");

        // __init__ — default constructor (always generate unless user wrote a no-arg ctor)
        boolean hasUserDefaultCtor = pc.methods.stream()
                .anyMatch(m -> m.isConstructor && m.params.isEmpty());
        List<ParsedAttribute> instanceAttrs = pc.attributes.stream()
                .filter(a -> !a.isStatic).toList();

        if (!hasUserDefaultCtor) {
            sb.append("    def __init__(self):\n");
            if (!instanceAttrs.isEmpty()) {
                for (ParsedAttribute a : instanceAttrs) {
                    sb.append("        self.").append(pyAttrName(a)).append(" = ").append(pyDefault(a.type)).append("\n");
                }
            } else {
                sb.append("        pass\n");
            }
            sb.append("\n");
        }

        // User-defined methods
        for (ParsedMethod m : pc.methods) {
            if (m.isAbstract) sb.append("    @abstractmethod\n");
            if (m.isStatic) sb.append("    @staticmethod\n");

            if (m.isConstructor) {
                sb.append("    def __init__(self");
                for (String[] p : m.params) sb.append(", ").append(p[0]);
                sb.append("):\n");
                if (m.params.isEmpty()) {
                    sb.append("        pass\n\n");
                } else {
                    for (String[] p : m.params) {
                        sb.append("        self.").append(p[0]).append(" = ").append(p[0]).append("\n");
                    }
                    sb.append("\n");
                }
            } else {
                if (m.isStatic) {
                    sb.append("    def ").append(m.name).append("(");
                } else {
                    sb.append("    def ").append(m.name).append("(self");
                    if (!m.params.isEmpty()) sb.append(", ");
                }
                for (int i = 0; i < m.params.size(); i++) {
                    if (i > 0 || !m.isStatic) { if (i > 0) sb.append(", "); }
                    else if (i > 0) sb.append(", ");
                    sb.append(m.params.get(i)[0]);
                }
                sb.append("):\n");
                if (m.isAbstract) {
                    sb.append("        pass\n\n");
                } else {
                    sb.append("        # TODO: implement\n");
                    sb.append("        pass\n\n");
                }
            }
        }

        // Getters/setters (property style)
        for (ParsedAttribute a : instanceAttrs) {
            String propName = a.name.startsWith("_") ? a.name.substring(a.name.startsWith("__") ? 2 : 1) : a.name;
            sb.append("    @property\n");
            sb.append("    def ").append(propName).append("(self):\n");
            sb.append("        return self.").append(pyAttrName(a)).append("\n\n");
            if (!a.isFinal) {
                sb.append("    @").append(propName).append(".setter\n");
                sb.append("    def ").append(propName).append("(self, value):\n");
                sb.append("        self.").append(pyAttrName(a)).append(" = value\n\n");
            }
        }

        if (pc.attributes.isEmpty() && pc.methods.isEmpty()) {
            sb.append("    pass\n");
        }

        return sb.toString();
    }

    private String pyAttrName(ParsedAttribute a) {
        if ("private".equals(a.access)) return "__" + a.name;
        if ("protected".equals(a.access)) return "_" + a.name;
        return a.name;
    }

    private String pyDefault(String type) {
        return switch (type.toLowerCase()) {
            case "int", "long", "short", "byte", "float", "double" -> "0";
            case "boolean", "bool" -> "False";
            case "string", "str" -> "\"\"";
            default -> "None";
        };
    }

    // ════════════════ C++ ════════════════
    private String generateCpp(ParsedClass pc) {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <iostream>\n#include <string>\nusing namespace std;\n\n");

        boolean isAbstract = "abstract".equals(pc.stereotype);
        sb.append("class ").append(pc.name).append(" {\n");

        // Default constructor (always generate unless user wrote no-arg ctor)
        boolean hasUserDefaultCtor = pc.methods.stream()
                .anyMatch(m -> m.isConstructor && m.params.isEmpty());
        if (!hasUserDefaultCtor && !isAbstract) {
            sb.append("public:\n");
            sb.append("    ").append(pc.name).append("() {\n");
            sb.append("        // Default constructor\n");
            sb.append("    }\n\n");
        }

        // Group by access
        appendCppSection(sb, pc, "public");
        appendCppSection(sb, pc, "protected");
        appendCppSection(sb, pc, "private");
        appendCppSection(sb, pc, "");  // no access modifier specified

        sb.append("};\n");
        return sb.toString();
    }

    private void appendCppSection(StringBuilder sb, ParsedClass pc, String access) {
        List<ParsedAttribute> attrs = pc.attributes.stream()
                .filter(a -> a.access.equals(access)).toList();
        List<ParsedMethod> meths = pc.methods.stream()
                .filter(m -> m.access.equals(access)).toList();
        if (attrs.isEmpty() && meths.isEmpty()) return;

        if (!access.isEmpty()) {
            sb.append(access).append(":\n");
        } else {
            sb.append("// (default access)\n");
        }
        for (ParsedAttribute a : attrs) {
            sb.append("    ");
            if (a.isStatic) sb.append("static ");
            if (a.isFinal) sb.append("const ");
            sb.append(cppType(a.type)).append(" ").append(a.name);
            if (a.defaultValue != null) sb.append(" = ").append(a.defaultValue);
            sb.append(";\n");
        }
        for (ParsedMethod m : meths) {
            sb.append("    ");
            if (m.isStatic) sb.append("static ");
            if (m.isAbstract) sb.append("virtual ");
            if (m.isConstructor) {
                sb.append(pc.name).append("(");
            } else {
                String rt = m.returnType.isEmpty() ? "void" : cppType(m.returnType);
                sb.append(rt).append(" ").append(m.name).append("(");
            }
            for (int i = 0; i < m.params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(cppType(m.params.get(i)[1])).append(" ").append(m.params.get(i)[0]);
            }
            sb.append(")");
            if (m.isAbstract) {
                sb.append(" = 0;\n");
            } else {
                sb.append(" {\n");
                if (m.isConstructor) {
                    for (String[] p : m.params) {
                        sb.append("        this->").append(p[0]).append(" = ").append(p[0]).append(";\n");
                    }
                } else {
                    sb.append("        // TODO: implement\n");
                }
                sb.append("    }\n");
            }
        }
        sb.append("\n");
    }

    private String cppType(String umlType) {
        return switch (umlType.toLowerCase()) {
            case "string" -> "string";
            case "boolean", "bool" -> "bool";
            case "integer" -> "int";
            default -> umlType;
        };
    }

    // ──────────────── HELPERS ────────────────

    private String extractModifiers(String line) {
        int brace = line.lastIndexOf('{');
        int braceEnd = line.lastIndexOf('}');
        if (brace >= 0 && braceEnd > brace) {
            return line.substring(brace + 1, braceEnd).toLowerCase();
        }
        return "";
    }

    private String removeModifiers(String line) {
        int brace = line.lastIndexOf('{');
        if (brace >= 0) {
            int end = line.lastIndexOf('}');
            if (end > brace) return (line.substring(0, brace) + line.substring(end + 1)).trim();
        }
        return line;
    }

    private String parseAccess(String line) {
        line = line.trim();
        if (line.startsWith("+")) return "public";
        if (line.startsWith("-")) return "private";
        if (line.startsWith("#")) return "protected";
        if (line.startsWith("~")) return "package";
        return "";
    }

    private String stripAccessSymbol(String line) {
        line = line.trim();
        if (!line.isEmpty() && "+-#~".indexOf(line.charAt(0)) >= 0) {
            return line.substring(1).trim();
        }
        return line;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
