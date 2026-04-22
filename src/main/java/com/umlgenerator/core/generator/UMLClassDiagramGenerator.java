package com.umlgenerator.core.generator;

import com.umlgenerator.core.model.*;

/**
 * Generates UML Class Diagrams from parsed code.
 * Handles layout hinting for the rendering layer.
 */
public class UMLClassDiagramGenerator extends DiagramGenerator<UMLDiagram> {

    @Override
    public String getGeneratorName() {
        return "UML Class Diagram Generator";
    }

    @Override
    protected void validate(Object input) {
        super.validate(input);
        if (!(input instanceof UMLDiagram)) {
            throw new IllegalArgumentException("Input must be a UMLDiagram");
        }
    }

    @Override
    protected UMLDiagram doGenerate(Object input) {
        // The actual parsing is done by LanguageParser
        // This generator adds layout and organization logic
        // Input is already a UMLDiagram from the parser
        if (input instanceof UMLDiagram diagram) {
            return organizeDiagram(diagram);
        }
        return new UMLDiagram("Empty", "Unknown");
    }

    /**
     * Organize diagram for optimal display.
     * - Sort classes by hierarchy level
     * - Group related classes together
     * - Detect overview vs detailed mode
     */
    private UMLDiagram organizeDiagram(UMLDiagram diagram) {
        // Mark abstract classes that need the <<abstract>> stereotype
        // Also clean up Main/Runner/Test classes by keeping only essential methods (constructors + main)
        for (UMLClass cls : diagram.getClasses()) {
            if (cls.getClassType() == ClassType.CLASS && cls.hasAbstractMethods()) {
                cls.setClassType(ClassType.ABSTRACT_CLASS);
            }
            
            String nameLower = cls.getName().toLowerCase();
            if (nameLower.contains("main") || nameLower.contains("runner") || nameLower.contains("test")) {
                cls.getMethods().removeIf(m -> !m.isConstructor() && !m.getName().equals("main"));
            }
        }

        // Filter out UI components (buttons, panels, etc.) from attributes and methods
        for (UMLClass cls : diagram.getClasses()) {
            cls.getAttributes().removeIf(attr -> isUIElement(attr.getName(), attr.getType()));
            cls.getMethods().removeIf(m -> isUIElement(m.getName(), m.getReturnType()) || 
                                           m.getParameters().stream().anyMatch(p -> isUIElement(p.getName(), p.getType())));
        }

        // Remove duplicate relationships and relationships to UI types
        var validRels = diagram.getRelationships().stream()
                .filter(rel -> !isUIElement(null, rel.getTargetClassName()) && !isUIElement(null, rel.getSourceClassName()))
                .distinct()
                .toList();
        diagram.setRelationships(new java.util.ArrayList<>(validRels));

        return diagram;
    }

    private boolean isUIElement(String name, String type) {
        String n = name != null ? name.toLowerCase() : "";
        String t = type != null ? type.toLowerCase() : "";
        
        // Strip generics and arrays
        t = t.replaceAll("<.*>", "").replace("[]", "").trim();

        // Check if name strongly suggests a UI element (if type is missing or vague)
        // Also catch parser glitches for local variables/keywords like break, stream
        if (n.endsWith("btn") || n.endsWith("button") || n.endsWith("panel") || 
            n.endsWith("layout") || n.endsWith("box") || n.endsWith("scene") || 
            n.endsWith("view") || n.endsWith("spacer") || n.equals("sep") ||
            n.endsWith("card") || n.endsWith("list") || n.endsWith("area") || 
            n.endsWith("header") || n.endsWith("footer") || n.equals("s") ||
            n.equals("break") || n.equals("continue") || n.equals("return") || 
            n.equals("stream") || n.equals("columns")) {
            return true;
        }
        
        // Common Java Swing/FX and UI types
        return t.endsWith("button") || t.endsWith("panel") || t.endsWith("pane") ||
               t.endsWith("label") || t.endsWith("textfield") || t.endsWith("textarea") ||
               t.endsWith("checkbox") || t.endsWith("radiobutton") || t.endsWith("combobox") ||
               t.endsWith("menu") || t.endsWith("menubar") || t.endsWith("menuitem") ||
               t.endsWith("toolbar") || t.endsWith("window") || t.endsWith("dialog") ||
               t.endsWith("frame") || t.endsWith("canvas") || t.endsWith("imageview") ||
               t.endsWith("scene") || t.endsWith("stage") || t.endsWith("slider") ||
               t.endsWith("spinner") || t.endsWith("scrollpane") || t.endsWith("listview") ||
               t.endsWith("tableview") || t.equals("text") || t.equals("font") ||
               t.equals("color") || t.equals("graphics") || t.equals("graphics2d") ||
               t.equals("jcomponent") || t.equals("component") || t.equals("container") ||
               t.equals("vbox") || t.equals("hbox") || t.equals("region") || 
               t.equals("separator") || t.equals("rectangle") || t.equals("circle") || t.equals("shape");
    }

    @Override
    protected void postProcess(UMLDiagram result) {
        // Ensure all classes referenced in relationships exist in the diagram
        var classNames = result.getClasses().stream()
                .map(UMLClass::getName)
                .collect(java.util.stream.Collectors.toSet());

        // Add placeholder classes for external references
        for (UMLRelationship rel : result.getRelationships()) {
            if (!classNames.contains(rel.getTargetClassName())) {
                UMLClass external = new UMLClass(rel.getTargetClassName(), ClassType.CLASS);
                external.setPackageName("(external)");
                result.addClass(external);
                classNames.add(rel.getTargetClassName());
            }
        }
    }
}
