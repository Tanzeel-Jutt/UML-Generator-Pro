package com.umlgenerator.ui.components;

import com.umlgenerator.core.model.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.*;
import javafx.scene.Group;
import javafx.scene.input.ScrollEvent;

/**
 * Main diagram canvas for rendering UML class diagrams.
 * Supports two modes:
 * - Detail mode (≤20 classes): full UML with all attributes/methods
 * - Overview mode (>20 classes): compact boxes, click to expand
 * 
 * Features:
 * - Auto-layout algorithm (tree + grid hybrid)
 * - Zoom and pan support
 * - Interactive class box clicking
 */
public class DiagramCanvas extends ScrollPane {

    private final Pane canvas;
    private UMLDiagram diagram;
    private final Map<String, ClassBoxNode> classBoxMap = new LinkedHashMap<>();
    private Runnable onClassClickHandler;
    private String selectedClassName;

    // Layout constants
    private static final double HORIZONTAL_SPACING = 150;
    private static final double VERTICAL_SPACING = 150; 
    private static final double PADDING = 100;
    private static final double DEFAULT_BOX_WIDTH = 280;
    private static final double MIN_BOX_HEIGHT = 180;

    public DiagramCanvas() {
        canvas = new Pane();
        canvas.setStyle("-fx-background-color: #0f172a;");
        canvas.setMinSize(1200, 800);
        
        // Wrap canvas in a group for smooth zooming
        Group zoomGroup = new Group(canvas);
        setContent(zoomGroup);
        
        setFitToWidth(true);
        setFitToHeight(true);
        setPannable(true);
        setStyle("-fx-background: #0f172a; -fx-border-color: transparent;");

        // Zoom feature
        addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                event.consume();
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                double currentScale = canvas.getScaleX();
                double newScale = Math.max(0.2, Math.min(3.0, currentScale * zoomFactor));
                canvas.setScaleX(newScale);
                canvas.setScaleY(newScale);
            }
        });
    }

    /**
     * Set handler called when a class box is clicked (in overview mode).
     */
    public void setOnClassClick(Runnable handler) {
        this.onClassClickHandler = handler;
    }

    public String getSelectedClassName() {
        return selectedClassName;
    }

    /**
     * Render a UML diagram with auto-layout.
     */
    public void renderDiagram(UMLDiagram diagram) {
        this.diagram = diagram;
        canvas.getChildren().clear();
        classBoxMap.clear();

        if (diagram == null || diagram.getClasses().isEmpty()) {
            showEmptyMessage();
            return;
        }

        boolean overviewMode = diagram.shouldUseOverviewMode();

        // Create class box nodes
        for (UMLClass cls : diagram.getClasses()) {
            ClassBoxNode box = new ClassBoxNode(cls, overviewMode);
            box.setOnClickHandler(() -> {
                selectedClassName = cls.getName();
                if (onClassClickHandler != null) onClassClickHandler.run();
            });
            classBoxMap.put(cls.getName(), box);
            canvas.getChildren().add(box);
        }

        // Calculate and apply layout
        layoutDiagram();

        // Force layout pass before drawing relationships
        canvas.applyCss();
        canvas.layout();

        // Draw relationships after a brief delay to ensure layout is complete
        javafx.application.Platform.runLater(() -> {
            drawRelationships();
        });

        // Add title
        Label title = new Label(diagram.getTitle());
        title.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-style: italic;");
        title.setLayoutX(10);
        title.setLayoutY(5);
        canvas.getChildren().add(title);

        // Add mode indicator
        String modeText = overviewMode
                ? "📦 Overview Mode - Click a class to see details"
                : "📋 Detail Mode - Showing all class details";
        Label modeLabel = new Label(modeText);
        modeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        modeLabel.setLayoutX(10);
        modeLabel.setLayoutY(20);
        canvas.getChildren().add(modeLabel);
    }

    /**
     * Show detail view for a single class.
     */
    public void showClassDetail(UMLClass umlClass) {
        canvas.getChildren().clear();
        classBoxMap.clear();

        // Create full detail box
        ClassBoxNode detailBox = new ClassBoxNode(umlClass, false);
        detailBox.setLayoutX(PADDING);
        detailBox.setLayoutY(PADDING + 30);
        detailBox.setMinWidth(350);
        canvas.getChildren().add(detailBox);
        classBoxMap.put(umlClass.getName(), detailBox);

        // Show related classes with generous spacing for clean arrows
        if (diagram != null) {
            List<UMLRelationship> rels = diagram.getRelationshipsFor(umlClass.getName());
            double offsetY = PADDING + 30;
            double offsetX = 650;  // far enough from detail box for arrow clearance
            double relSpacing = 250; // generous vertical gap between related boxes

            for (UMLRelationship rel : rels) {
                String relatedName = rel.getSourceClassName().equals(umlClass.getName())
                        ? rel.getTargetClassName() : rel.getSourceClassName();

                diagram.findClass(relatedName).ifPresent(relatedClass -> {
                    if (!classBoxMap.containsKey(relatedName)) {
                        ClassBoxNode relBox = new ClassBoxNode(relatedClass, true);
                        relBox.setLayoutX(offsetX);
                        relBox.setLayoutY(offsetY + (classBoxMap.size() - 1) * relSpacing);
                        relBox.setOnClickHandler(() -> {
                            selectedClassName = relatedName;
                            if (onClassClickHandler != null) onClassClickHandler.run();
                        });
                        canvas.getChildren().add(relBox);
                        classBoxMap.put(relatedName, relBox);
                    }
                });
            }

            // Force layout pass before drawing relationships so dimensions are correct
            canvas.applyCss();
            canvas.layout();
            
            // Draw relationships after layout
            javafx.application.Platform.runLater(this::drawRelationships);
        }

    }

    /**
     * Auto-layout algorithm: hierarchical tree layout + grid fallback.
     */
    private void layoutDiagram() {
        if (diagram == null) return;

        // Build adjacency for hierarchy
        List<UMLClass> rootClasses = diagram.getRootClasses();
        Set<String> positioned = new HashSet<>();

        double currentX = PADDING;
        double currentY = PADDING + 40;

        // Phase 1: Layout hierarchy trees
        for (UMLClass root : rootClasses) {
            double subtreeWidth = layoutSubtree(root.getName(), currentX, currentY, positioned);
            currentX += subtreeWidth + HORIZONTAL_SPACING;
        }

        // Phase 2: Grid layout for unpositioned classes
        List<UMLClass> remaining = diagram.getClasses().stream()
                .filter(c -> !positioned.contains(c.getName()))
                .toList();

        if (!remaining.isEmpty()) {
            int cols = Math.max(1, (int) Math.ceil(Math.sqrt(remaining.size())));
            double gridStartX = PADDING;
            double maxTreeY = currentY;
            for (ClassBoxNode node : classBoxMap.values()) {
                if (positioned.contains(node.getUmlClass().getName())) {
                    maxTreeY = Math.max(maxTreeY, node.getLayoutY() + estimateBoxHeight(node.getUmlClass()));
                }
            }
            double gridStartY = positioned.isEmpty() ? PADDING + 40 : maxTreeY + VERTICAL_SPACING;

            double currentGridY = gridStartY;
            double currentGridX = gridStartX;
            double currentRowMaxHeight = 0;

            for (int i = 0; i < remaining.size(); i++) {
                int col = i % cols;
                if (col == 0 && i > 0) {
                    currentGridY += currentRowMaxHeight + VERTICAL_SPACING;
                    currentGridX = gridStartX;
                    currentRowMaxHeight = 0;
                }

                ClassBoxNode box = classBoxMap.get(remaining.get(i).getName());
                if (box != null) {
                    box.applyCss();
                    box.layout();
                    double boxWidth = Math.max(DEFAULT_BOX_WIDTH, box.prefWidth(-1));
                    double boxHeight = Math.max(MIN_BOX_HEIGHT, box.prefHeight(-1));

                    box.setLayoutX(currentGridX);
                    box.setLayoutY(currentGridY);
                    positioned.add(remaining.get(i).getName());
                    
                    currentRowMaxHeight = Math.max(currentRowMaxHeight, boxHeight);
                    currentGridX += boxWidth + HORIZONTAL_SPACING;
                }
            }
        }

        // Resize canvas to fit
        double maxX = 0, maxY = 0;
        for (ClassBoxNode box : classBoxMap.values()) {
            maxX = Math.max(maxX, box.getLayoutX() + 350);
            maxY = Math.max(maxY, box.getLayoutY() + 300);
        }
        canvas.setMinSize(Math.max(1200, maxX + PADDING), Math.max(800, maxY + PADDING));
    }

    /**
     * Layout a subtree rooted at the given class.
     */
    private double layoutSubtree(String className, double x, double y, Set<String> positioned) {
        if (positioned.contains(className)) return 0;

        ClassBoxNode box = classBoxMap.get(className);
        if (box == null) return 0;

        positioned.add(className);

        // Find children
        List<UMLClass> children = diagram.getChildClasses(className);
        // Also find implementors
        List<String> implementors = diagram.getRelationships().stream()
                .filter(r -> r.getType() == RelationshipType.IMPLEMENTATION
                        && r.getTargetClassName().equals(className))
                .map(UMLRelationship::getSourceClassName)
                .toList();

        List<String> allChildren = new ArrayList<>();
        children.forEach(c -> allChildren.add(c.getName()));
        allChildren.addAll(implementors);

        if (allChildren.isEmpty()) {
            box.setLayoutX(x);
            box.setLayoutY(y);
            return 280;
        }

        // Force layout to get exact dimensions
        box.applyCss();
        box.layout();
        double boxWidth = Math.max(DEFAULT_BOX_WIDTH, box.prefWidth(-1));
        double boxHeight = Math.max(MIN_BOX_HEIGHT, box.prefHeight(-1));

        // Layout children first to determine width
        double childX = x;
        double childY = y + boxHeight + VERTICAL_SPACING; // Dynamic height
        double totalChildWidth = 0;

        for (String childName : allChildren) {
            if (positioned.contains(childName)) continue;
            double childWidth = layoutSubtree(childName, childX, childY, positioned);
            childX += childWidth + HORIZONTAL_SPACING;
            totalChildWidth += childWidth + HORIZONTAL_SPACING;
        }

        if (totalChildWidth > 0) totalChildWidth -= HORIZONTAL_SPACING;

        // Center parent above children
        double parentX = x + Math.max(0, (totalChildWidth - boxWidth) / 2);
        box.setLayoutX(parentX);
        box.setLayoutY(y);

        return Math.max(boxWidth, totalChildWidth);
    }

    /**
     * Estimate the height of a class box based on its contents.
     */
    private double estimateBoxHeight(UMLClass cls) {
        double height = 60; // Header + Padding
        if (cls.getClassType() == ClassType.ENUM) {
            height += cls.getEnumConstants().size() * 18;
        }
        height += cls.getAttributes().size() * 18;
        height += cls.getMethods().size() * 18;
        return Math.max(MIN_BOX_HEIGHT, height + 40);
    }

    /**
     * Draw all relationship lines using slot-based renderer to prevent overlaps.
     */
    private void drawRelationships() {
        if (diagram == null) return;
        RelationshipRenderer.drawAllRelationships(canvas, diagram.getRelationships(), classBoxMap);
    }

    private void showEmptyMessage() {
        Label empty = new Label("No classes found. Paste your code and click Generate.");
        empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px;");
        empty.setLayoutX(100);
        empty.setLayoutY(100);
        canvas.getChildren().add(empty);
    }

    public Pane getCanvas() {
        return canvas;
    }
}
