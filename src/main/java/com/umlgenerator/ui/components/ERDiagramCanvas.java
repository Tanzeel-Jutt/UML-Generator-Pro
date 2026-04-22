package com.umlgenerator.ui.components;

import com.umlgenerator.core.model.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;

/**
 * Canvas component for rendering ER and EERD diagrams.
 * Draws entities as rectangles, relationships as diamonds,
 * and attributes as ellipses with proper ER notation.
 */
public class ERDiagramCanvas extends ScrollPane {

    private final Pane canvas;
    private ERDiagram diagram;
    private Map<String, double[]> entityPositions = new HashMap<>();
    private Map<String, Integer> entityAttrSides = new HashMap<>(); // 1 for right, -1 for left
    private int maxAttributesCount = 1;

    // Colors
    private static final Color ENTITY_COLOR = Color.web("#3b82f6");
    private static final Color WEAK_ENTITY_COLOR = Color.web("#f59e0b");
    private static final Color RELATIONSHIP_COLOR = Color.web("#10b981");
    private static final Color ATTRIBUTE_COLOR = Color.web("#e2e8f0");
    private static final Color PK_COLOR = Color.web("#fbbf24");
    private static final Color FK_COLOR = Color.web("#f87171");
    private static final Color MULTI_VALUED_COLOR = Color.web("#a78bfa");
    private static final Color DERIVED_COLOR = Color.web("#6b7280");
    private static final Color BG_COLOR = Color.web("#0f172a");

    public ERDiagramCanvas() {
        canvas = new Pane();
        canvas.setStyle("-fx-background-color: #0f172a;");
        canvas.setMinSize(1200, 800);
        setContent(canvas);
        setFitToWidth(true);
        setFitToHeight(true);
        setPannable(true);
        setStyle("-fx-background: #0f172a; -fx-border-color: transparent;");

        // Zoom feature
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
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
     * Render an ER diagram.
     */
    public void renderDiagram(ERDiagram diagram) {
        this.diagram = diagram;
        canvas.getChildren().clear();
        entityPositions.clear();

        if (diagram == null || diagram.getEntities().isEmpty()) return;

        // Calculate layout positions
        calculateLayout();

        // Draw relationships first (behind entities)
        int relIdx = 0;
        Map<String, Integer> entityExitCounts = new java.util.HashMap<>();
        for (ERRelationship rel : diagram.getRelationships()) {
            drawERRelationship(rel, relIdx++, entityExitCounts);
        }

        // Draw entities with attributes
        for (EREntity entity : diagram.getEntities()) {
            if (diagram.isEnhanced()) {
                drawUMLBox(entity);
            } else {
                drawEntity(entity);
            }
        }

        // Draw specialization if EERD
        if (diagram.isEnhanced()) {
            for (EREntity entity : diagram.getEntities()) {
                if (entity.hasSpecialization()) {
                    drawSpecialization(entity);
                }
            }
        }
    }

    private void calculateLayout() {
        List<EREntity> entities = diagram.getEntities();
        maxAttributesCount = entities.stream().mapToInt(e -> e.getAttributes() != null ? e.getAttributes().size() : 0).max().orElse(1);

        if (diagram.isEnhanced()) {
            // Separate parents, children, and standalone entities
            List<EREntity> parentEnts = new java.util.ArrayList<>();
            java.util.Set<String> childNames = new java.util.HashSet<>();
            
            for (EREntity e : entities) {
                if (e.hasSpecialization() && e.getChildEntities() != null && !e.getChildEntities().isEmpty()) {
                    parentEnts.add(e);
                    childNames.addAll(e.getChildEntities());
                }
            }
            
            List<EREntity> standaloneEnts = entities.stream()
                .filter(e -> !childNames.contains(e.getName()) && !parentEnts.contains(e))
                .collect(java.util.stream.Collectors.toList());
            
            int maxChildren = 0;
            for (EREntity e : entities) {
                if (e.hasSpecialization() && e.getChildEntities() != null) {
                    maxChildren = Math.max(maxChildren, e.getChildEntities().size());
                }
            }
            double eerdChildSpacing = 400;
            double requiredLeftMargin = Math.max(350, (maxChildren > 1 ? ((maxChildren - 1) * eerdChildSpacing) / 2 + 200 : 350));
            double startX = requiredLeftMargin, startY = 280;
            
            double spacingX = 1200;
            double spacingY = 0;
            for (EREntity e : entities) {
                spacingY = Math.max(spacingY, getTotalHalfHeight(e) * 2 + 250);
            }
            spacingY = Math.max(spacingY, 700);

            int standaloneCols = Math.max(1, Math.min(3, standaloneEnts.size()));
            
            // Layout standalone entities in a grid
            for (int i = 0; i < standaloneEnts.size(); i++) {
                int row = i / standaloneCols, col = i % standaloneCols;
                double x = startX + col * spacingX;
                double y = startY + row * spacingY;
                entityPositions.put(standaloneEnts.get(i).getName(), new double[]{x, y});
                entityAttrSides.put(standaloneEnts.get(i).getName(), col % 2 == 0 ? 1 : -1);
            }
            
            // Layout parent entities and their children hierarchically
            double groupY = startY + (standaloneEnts.isEmpty() ? 0 : (Math.ceil((double)standaloneEnts.size() / standaloneCols) * spacingY + 50));
            double parentSpacingX = spacingX;
            
            for (int pi = 0; pi < parentEnts.size(); pi++) {
                EREntity parent = parentEnts.get(pi);
                double px = startX + pi * parentSpacingX;
                double py = groupY;
                entityPositions.put(parent.getName(), new double[]{px, py});
                entityAttrSides.put(parent.getName(), pi % 2 == 0 ? 1 : -1);
                
                // Place children below parent, spread horizontally
                List<String> childList = parent.getChildEntities();
                if (childList != null && !childList.isEmpty()) {
                    double childSpacing = 400;
                    double childStartX = px - ((childList.size() - 1) * childSpacing) / 2.0;
                    double parentTotalH = getTotalHalfHeight(parent) * 2;
                    double childY = py + parentTotalH / 2 + 200; // Safe distance below parent
                    
                    for (int ci = 0; ci < childList.size(); ci++) {
                        double cx = childStartX + ci * childSpacing;
                        entityPositions.put(childList.get(ci), new double[]{cx, childY});
                        entityAttrSides.put(childList.get(ci), ci % 2 == 0 ? 1 : -1);
                    }
                }
                if (parentEnts.size() > 2 && pi % 2 == 1) groupY += spacingY + 200;
            }
            
            double maxX = entityPositions.values().stream().mapToDouble(p -> p[0]).max().orElse(800);
            double maxY = entityPositions.values().stream().mapToDouble(p -> p[1]).max().orElse(600);
            canvas.setMinWidth(maxX + 400);
            canvas.setMinHeight(maxY + 400);
        } else {
            // ═══ Standard ER Grid Layout ═══
            int cols = Math.max(1, (int) Math.ceil(Math.sqrt(entities.size())));
            double spacingX = 1200;
            
            double maxH = 0;
            for (EREntity e : entities) {
                maxH = Math.max(maxH, getTotalHalfHeight(e) * 2);
            }
            double spacingY = Math.max(700, maxH + 200);
            double startX = 350;
            double startY = 280;

            for (int i = 0; i < entities.size(); i++) {
                int row = i / cols;
                int col = i % cols;
                double x = startX + col * spacingX;
                double y = startY + row * spacingY;
                entityPositions.put(entities.get(i).getName(), new double[]{x, y});
                entityAttrSides.put(entities.get(i).getName(), col % 2 == 0 ? 1 : -1);
            }

            canvas.setMinWidth(startX + cols * spacingX + 200);
            canvas.setMinHeight(startY + ((entities.size() / cols) + 1) * spacingY + 200);
        }
    }

    private void drawUMLBox(EREntity entity) {
        double[] pos = entityPositions.get(entity.getName());
        if (pos == null) return;
        
        double width = getEERDBoxWidth(entity);
        double height = getEERDBoxHeight(entity);
        double x = pos[0] - width / 2;
        double y = pos[1] - height / 2;
        
        VBox box = new VBox();
        box.setLayoutX(x);
        box.setLayoutY(y);
        box.setMinWidth(width);
        box.setMinHeight(height);
        
        box.setStyle("-fx-background-color: #1e293b; -fx-border-color: " + 
                     (entity.isWeak() ? "#f59e0b" : "#3b82f6") + 
                     "; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6;");
                     
        if (entity.isWeak()) {
            box.setEffect(new javafx.scene.effect.InnerShadow(5, Color.web("#f59e0b")));
        }
        
        // Header
        Label nameLabel = new Label(entity.getName());
        nameLabel.setFont(Font.font("Inter", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setPadding(new Insets(6, 10, 6, 10));
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setAlignment(javafx.geometry.Pos.CENTER);
        nameLabel.setStyle("-fx-border-color: transparent transparent #334155 transparent; -fx-border-width: 0 0 1 0;");
        
        box.getChildren().add(nameLabel);
        
        // Attributes
        VBox attrsBox = new VBox(2);
        attrsBox.setPadding(new Insets(5, 10, 5, 10));
        
        if (entity.getAttributes().isEmpty()) {
            Label emptyLbl = new Label("(no attributes)");
            emptyLbl.setTextFill(Color.web("#64748b"));
            emptyLbl.setFont(Font.font("Consolas", javafx.scene.text.FontPosture.ITALIC, 11));
            attrsBox.getChildren().add(emptyLbl);
        } else {
            for (ERAttribute attr : entity.getAttributes()) {
                String prefix = attr.isMultiValued() ? "{MV} " : (attr.isDerived() ? "/ " : "");
                String text = prefix + attr.getName() + (attr.getDataType() != null && !attr.getDataType().isEmpty() ? ": " + attr.getDataType() : "");
                
                Label attrLbl = new Label(text);
                attrLbl.setFont(Font.font("Consolas", 11));
                
                if (attr.isPrimaryKey()) {
                    attrLbl.setTextFill(Color.web("#fbbf24"));
                    attrLbl.setUnderline(true);
                } else if (attr.isForeignKey()) {
                    attrLbl.setTextFill(Color.web("#f87171"));
                } else {
                    attrLbl.setTextFill(Color.web("#e2e8f0"));
                }
                
                if (attr.isDerived()) {
                    attrLbl.setStyle("-fx-border-color: transparent transparent #6b7280 transparent; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;");
                    attrLbl.setFont(Font.font("Consolas", javafx.scene.text.FontPosture.ITALIC, 11));
                }
                
                attrsBox.getChildren().add(attrLbl);
            }
        }
        
        box.getChildren().add(attrsBox);
        
        // Add drop shadow
        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.3));
        box.setEffect(shadow);
        
        canvas.getChildren().add(box);
    }

    private void drawEntity(EREntity entity) {
        double[] pos = entityPositions.get(entity.getName());
        if (pos == null) return;
        double x = pos[0], y = pos[1];
        double width = getERBoxWidth(entity), height = 40;

        // Entity rectangle
        Rectangle rect = new Rectangle(x - width / 2, y - height / 2, width, height);
        rect.setFill(Color.web("#1e293b"));
        rect.setStroke(entity.isWeak() ? WEAK_ENTITY_COLOR : ENTITY_COLOR);
        rect.setStrokeWidth(entity.isWeak() ? 3 : 2);
        rect.setArcWidth(8);
        rect.setArcHeight(8);

        DropShadow shadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.4));
        rect.setEffect(shadow);
        canvas.getChildren().add(rect);

        // Double border for weak entity
        if (entity.isWeak()) {
            Rectangle innerRect = new Rectangle(x - width / 2 + 4, y - height / 2 + 4,
                    width - 8, height - 8);
            innerRect.setFill(Color.TRANSPARENT);
            innerRect.setStroke(WEAK_ENTITY_COLOR);
            innerRect.setStrokeWidth(1.5);
            innerRect.setArcWidth(4);
            innerRect.setArcHeight(4);
            canvas.getChildren().add(innerRect);
        }

        // Entity name
        Label nameLabel = new Label(entity.getName());
        nameLabel.setFont(Font.font("Inter", FontWeight.BOLD, 13));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setLayoutX(x - measureTextWidth(entity.getName(), 13) / 2);
        nameLabel.setLayoutY(y - 9);
        canvas.getChildren().add(nameLabel);

        // Draw attributes around the entity
        drawEntityAttributes(entity, x, y, width, height);
    }

    private void drawEntityAttributes(EREntity entity, double entityX, double entityY,
                                       double entityW, double entityH) {
        List<ERAttribute> attrs = entity.getAttributes();
        double attrSpacing = 42;
        double attrStartY = entityY - ((attrs.size() - 1) * attrSpacing) / 2.0;
        
        int side = entityAttrSides.getOrDefault(entity.getName(), 1);
        double entityEdgeX = entityX + side * (entityW / 2);

        for (int i = 0; i < attrs.size(); i++) {
            ERAttribute attr = attrs.get(i);
            double ay = attrStartY + i * attrSpacing;
            
            double textW = measureTextWidth(attr.getName(), attr.isPrimaryKey() ? 12 : 10);
            double ew = Math.max(55, textW / 2 + 15);
            double eh = 18;
            
            double attrX = entityX + side * (entityW / 2 + 40 + ew);

            // Line from entity to attribute
            Line line = new Line(entityEdgeX, entityY, attrX, ay);
            line.setStroke(Color.web("#475569"));
            line.setStrokeWidth(1.2);
            canvas.getChildren().add(line);

            // Attribute ellipse
            Ellipse ellipse = new Ellipse(attrX, ay, ew, eh);
            ellipse.setFill(Color.web("#1e293b"));

            if (attr.isMultiValued()) {
                ellipse.setStroke(MULTI_VALUED_COLOR);
                ellipse.setStrokeWidth(2);
                // Double ellipse for multi-valued
                Ellipse inner = new Ellipse(attrX, ay, ew - 4, eh - 3);
                inner.setFill(Color.TRANSPARENT);
                inner.setStroke(MULTI_VALUED_COLOR);
                inner.setStrokeWidth(1);
                canvas.getChildren().add(inner);
            } else if (attr.isDerived()) {
                ellipse.setStroke(DERIVED_COLOR);
                ellipse.setStrokeWidth(1.5);
                ellipse.getStrokeDashArray().addAll(5.0, 3.0);
            } else if (attr.isPrimaryKey()) {
                ellipse.setStroke(PK_COLOR);
                ellipse.setStrokeWidth(2);
            } else if (attr.isForeignKey()) {
                ellipse.setStroke(FK_COLOR);
                ellipse.setStrokeWidth(1.5);
            } else {
                ellipse.setStroke(Color.web("#64748b"));
                ellipse.setStrokeWidth(1);
            }
            canvas.getChildren().add(ellipse);

            // Attribute name (underline if PK)
            Label attrLabel = new Label(attr.getName());
            attrLabel.setFont(Font.font("Inter", attr.isPrimaryKey() ? FontWeight.BOLD : FontWeight.NORMAL, attr.isPrimaryKey() ? 12 : 10));
            attrLabel.setTextFill(attr.isPrimaryKey() ? PK_COLOR : ATTRIBUTE_COLOR);
            attrLabel.setUnderline(attr.isPrimaryKey());
            attrLabel.setLayoutX(attrX - measureTextWidth(attr.getName(), attr.isPrimaryKey() ? 12 : 10) / 2);
            attrLabel.setLayoutY(ay - 7);
            canvas.getChildren().add(attrLabel);
        }
    }


    private void drawERRelationship(ERRelationship rel, int relIdx, Map<String, Integer> entityExitCounts) {
        // Skip ISA relationships as they are drawn by drawSpecialization
        if (diagram.isEnhanced()) {
            boolean isISA = false;
            for (EREntity parent : diagram.getEntities()) {
                if (parent.getChildEntities() != null && parent.getChildEntities().contains(rel.getEntity1Name())) isISA = true;
                if (parent.getChildEntities() != null && parent.getChildEntities().contains(rel.getEntity2Name())) isISA = true;
            }
            if (isISA) return;
        }

        double[] pos1 = entityPositions.get(rel.getEntity1Name());
        double[] pos2 = entityPositions.get(rel.getEntity2Name());
        if (pos1 == null || pos2 == null) return;

        double bx1 = pos1[0], by1 = pos1[1];
        double bx2 = pos2[0], by2 = pos2[1];
        
        EREntity ent1 = diagram.getEntities().stream().filter(e -> e.getName().equals(rel.getEntity1Name())).findFirst().orElse(null);
        EREntity ent2 = diagram.getEntities().stream().filter(e -> e.getName().equals(rel.getEntity2Name())).findFirst().orElse(null);
        
        double totalHH1 = getTotalHalfHeight(ent1);
        double totalHH2 = getTotalHalfHeight(ent2);
        
        double coreHH1 = diagram.isEnhanced() ? totalHH1 : 20; // 20 is half of 40 (rectangle height)
        double coreHH2 = diagram.isEnhanced() ? totalHH2 : 20;
        
        double midX, midY;
        Path path1 = new Path();
        Path path2 = new Path();
        
        double exitX1, exitY1;
        double exitX2, exitY2;
        
        if (rel.getEntity1Name().equals(rel.getEntity2Name())) {
            double hw = diagram.isEnhanced() ? getEERDBoxWidth(ent1) / 2 + 10 : getERBoxWidth(ent1) / 2 + 10;
            path1.getElements().add(new MoveTo(bx1 + hw, by1));
            path1.getElements().add(new LineTo(bx1 + hw + 40, by1));
            path1.getElements().add(new LineTo(bx1 + hw + 40, by1 - coreHH1 - 40));
            
            path2.getElements().add(new MoveTo(bx1, by1 - coreHH1));
            path2.getElements().add(new LineTo(bx1, by1 - coreHH1 - 40));
            path2.getElements().add(new LineTo(bx1 + hw + 40, by1 - coreHH1 - 40));
            
            midX = bx1 + hw + 40;
            midY = by1 - coreHH1 - 40;
            
            exitX1 = bx1 + hw; exitY1 = by1;
            exitX2 = bx1; exitY2 = by1 - coreHH1;
        } else {
            int c1 = entityExitCounts.getOrDefault(rel.getEntity1Name(), 0);
            int c2 = entityExitCounts.getOrDefault(rel.getEntity2Name(), 0);
            entityExitCounts.put(rel.getEntity1Name(), c1 + 1);
            entityExitCounts.put(rel.getEntity2Name(), c2 + 1);
            
            // All lines exit from the exact center to form a single clean vertical trunk
            double offsetX1 = 0; 
            double offsetX2 = 0;
            
            // Universal horizontal lane routing that respects total entity height (including attributes)
            double laneY;
            if (Math.abs(by1 - by2) < 10) {
                // Same row: route in a lane completely below the entity bounds
                double maxTotalH = Math.max(totalHH1, totalHH2);
                if (!diagram.isEnhanced()) maxTotalH = Math.max(maxTotalH, 150);
                laneY = by1 + maxTotalH + 70 + (relIdx % 5) * 50;
            } else {
                // Different rows: route strictly in the safe gap between them
                double topY = Math.min(by1, by2);
                double botY = Math.max(by1, by2);
                double topTotalH = (topY == by1) ? totalHH1 : totalHH2;
                double botTotalH = (botY == by1) ? totalHH1 : totalHH2;
                
                double safeTop = topY + topTotalH + 50;
                double safeBottom = botY - botTotalH - 50;
                
                if (safeBottom <= safeTop) {
                    laneY = (topY + botY) / 2; // Fallback
                } else {
                    double gap = safeBottom - safeTop;
                    double fraction = 0.1 + (relIdx % 6) * 0.15; // Support up to 6 safe lanes
                    laneY = safeTop + gap * fraction;
                }
            }
            
            exitX1 = bx1 + offsetX1; exitY1 = (laneY > by1) ? by1 + coreHH1 : by1 - coreHH1;
            exitX2 = bx2 + offsetX2; exitY2 = (laneY > by2) ? by2 + coreHH2 : by2 - coreHH2;
            
            midX = (exitX1 + exitX2) / 2;
            // No diamond staggering needed since laneY naturally separates them!
            midY = laneY;
            
            path1.getElements().add(new MoveTo(exitX1, exitY1));
            path1.getElements().add(new LineTo(exitX1, laneY));
            path1.getElements().add(new LineTo(midX, laneY));
            
            path2.getElements().add(new MoveTo(exitX2, exitY2));
            path2.getElements().add(new LineTo(exitX2, laneY));
            path2.getElements().add(new LineTo(midX, laneY));
        }

        path1.setStroke(Color.web("#94a3b8"));
        path1.setStrokeWidth(rel.isIdentifying() ? 3 : 1.8);
        path1.setFill(null);
        path1.setStrokeLineJoin(StrokeLineJoin.ROUND);
        
        path2.setStroke(Color.web("#94a3b8"));
        path2.setStrokeWidth(rel.isIdentifying() ? 3 : 1.8);
        path2.setFill(null);
        path2.setStrokeLineJoin(StrokeLineJoin.ROUND);

        // Total participation = double line
        if (rel.isEntity1TotalParticipation()) path1.setStrokeWidth(4);
        if (rel.isEntity2TotalParticipation()) path2.setStrokeWidth(4);

        canvas.getChildren().addAll(path1, path2);

        // Diamond shape
        double size = 28;
        Polygon diamond = new Polygon(
                midX, midY - size,
                midX + size * 1.3, midY,
                midX, midY + size,
                midX - size * 1.3, midY
        );
        diamond.setFill(Color.web("#1e293b"));
        diamond.setStroke(rel.isIdentifying() ? Color.web("#f59e0b") : RELATIONSHIP_COLOR);
        diamond.setStrokeWidth(rel.isIdentifying() ? 3 : 2);

        DropShadow shadow = new DropShadow(6, Color.rgb(0, 0, 0, 0.3));
        diamond.setEffect(shadow);
        canvas.getChildren().add(diamond);

        if (rel.isIdentifying()) {
            Polygon innerDiamond = new Polygon(
                    midX, midY - size + 5,
                    midX + size * 1.3 - 6, midY,
                    midX, midY + size - 5,
                    midX - size * 1.3 + 6, midY
            );
            innerDiamond.setFill(Color.TRANSPARENT);
            innerDiamond.setStroke(RELATIONSHIP_COLOR);
            innerDiamond.setStrokeWidth(1.5);
            canvas.getChildren().add(innerDiamond);
        }

        Label relLabel = new Label(rel.getName() != null ? rel.getName() : "rel");
        relLabel.setFont(Font.font("Inter", FontWeight.BOLD, 11));
        relLabel.setTextFill(Color.WHITE);
        relLabel.setLayoutX(midX - measureTextWidth(relLabel.getText(), 11) / 2);
        relLabel.setLayoutY(midY - 7);
        canvas.getChildren().add(relLabel);

        // Cardinality labels
        boolean isLoop = rel.getEntity1Name() != null && rel.getEntity1Name().equals(rel.getEntity2Name());
        double e1x = exitX1;
        double e1y = exitY1;
        double e2x = exitX2;
        double e2y = exitY2;
        
        double l1x, l1y, l2x, l2y;
        
        if (isLoop) {
            l1y = e1y;
            l2y = e2y - 25;
            l1x = e1x + 20;
            l2x = e2x - 20;
        } else {
            // Anchor labels directly to the diamond (midX, midY) to guarantee no overlaps between different relationships
            // For Entity 1's label:
            if (Math.abs(midX - e1x) < 5) {
                // Perfectly vertical entry to the diamond
                l1x = e1x + 35;
                l1y = midY + (midY > e1y ? -55 : 55);
            } else {
                // Horizontal entry to the diamond
                l1y = midY - 18;
                l1x = midX + (midX > e1x ? -75 : 75);
            }
            
            // For Entity 2's label:
            if (Math.abs(midX - e2x) < 5) {
                // Perfectly vertical entry to the diamond
                l2x = e2x + 35;
                l2y = midY + (midY > e2y ? -55 : 55);
            } else {
                // Horizontal entry to the diamond
                l2y = midY - 18;
                l2x = midX + (midX > e2x ? -75 : 75);
            }
        }

        drawCardinalityLabel(rel.getCardinality1(), l1x, l1y);
        drawCardinalityLabel(rel.getCardinality2(), l2x, l2y);
    }
    
    private void drawCardinalityLabel(String text, double cx, double cy) {
        if (text == null || text.isEmpty()) return;
        
        Rectangle bg = new Rectangle(cx - 24, cy - 11, 48, 22);
        bg.setArcWidth(8);
        bg.setArcHeight(8);
        bg.setFill(Color.web("#1e293b"));
        bg.setStroke(Color.web("#334155"));
        bg.setStrokeWidth(1);
        canvas.getChildren().add(bg);

        Label label = new Label(text);
        label.setFont(Font.font("Inter", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#f43f5e"));
        label.setLayoutX(cx - measureTextWidth(text, 12) / 2);
        label.setLayoutY(cy - 8);
        canvas.getChildren().add(label);
    }
    
    // Replaced by Universal Gutter Lane Routing

    private void drawSpecialization(EREntity parent) {
        double[] parentPos = entityPositions.get(parent.getName());
        if (parentPos == null) return;

        double isaX = parentPos[0];
        double parentTotalH = getTotalHalfHeight(parent) * 2;
        // Position ISA triangle correctly below the parent's actual bottom
        double isaY = parentPos[1] + parentTotalH / 2 + 40;

        // ISA triangle
        double triSize = 20;
        Polygon triangle = new Polygon(
                isaX, isaY - triSize,
                isaX + triSize, isaY + triSize / 2,
                isaX - triSize, isaY + triSize / 2
        );
        triangle.setFill(Color.web("#1e293b"));
        triangle.setStroke(Color.web("#a78bfa"));
        triangle.setStrokeWidth(2);
        canvas.getChildren().add(triangle);

        // ISA label
        Label isaLabel = new Label("ISA");
        isaLabel.setFont(Font.font("Inter", FontWeight.BOLD, 9));
        isaLabel.setTextFill(Color.web("#a78bfa"));
        isaLabel.setLayoutX(isaX - 10);
        isaLabel.setLayoutY(isaY - 10);
        canvas.getChildren().add(isaLabel);

        // Line from parent to ISA
        Line parentLine = new Line(parentPos[0], parentPos[1] + parentTotalH / 2, isaX, isaY - triSize);
        parentLine.setStroke(Color.web("#a78bfa"));
        parentLine.setStrokeWidth(parent.isTotalSpecialization() ? 4.5 : 2); // Thick line = Total Specialization
        canvas.getChildren().add(parentLine);

        // Lines from ISA to children
        for (String childName : parent.getChildEntities()) {
            double[] childPos = entityPositions.get(childName);
            if (childPos != null) {
                EREntity child = diagram.getEntities().stream().filter(e -> e.getName().equals(childName)).findFirst().orElse(null);
                double childTop = childPos[1] - (child != null ? getEERDBoxHeight(child) / 2 : 20);
                Line childLine = new Line(isaX, isaY + triSize / 2, childPos[0], childTop);
                childLine.setStroke(Color.web("#a78bfa"));
                childLine.setStrokeWidth(1.5);
                canvas.getChildren().add(childLine);
            }
        }

        // Disjoint/Overlapping label
        String constraint = parent.isDisjointSpecialization() ? "d" : "o";
        Label constraintLabel = new Label(constraint);
        constraintLabel.setFont(Font.font("Inter", FontWeight.BOLD, 10));
        constraintLabel.setTextFill(Color.web("#a78bfa"));
        constraintLabel.setLayoutX(isaX + triSize + 5);
        constraintLabel.setLayoutY(isaY - 5);
        canvas.getChildren().add(constraintLabel);
    }
    private double getEERDBoxHeight(EREntity entity) {
        if (entity == null) return 100.0;
        int attrCount = (entity.getAttributes() != null) ? entity.getAttributes().size() : 0;
        return Math.max(60, 35 + Math.max(1, attrCount) * 22 + 18);
    }
    
    private double getEERDBoxWidth(EREntity entity) {
        if (entity == null) return 150.0;
        int maxLen = entity.getName().length();
        for (ERAttribute a : entity.getAttributes()) {
            maxLen = Math.max(maxLen, a.getName().length() + (a.getDataType() != null ? a.getDataType().length() : 0));
        }
        return 110 + maxLen * 5.5;
    }
    
    private double getERBoxWidth(EREntity entity) {
        if (entity == null || entity.getName() == null) return 150.0;
        int maxLen = entity.getName().length();
        return Math.max(150, maxLen * 8.5 + 40);
    }
    
    private double getTotalHalfHeight(EREntity entity) {
        if (entity == null) return 24;
        if (diagram.isEnhanced()) {
            return getEERDBoxHeight(entity) / 2.0;
        } else {
            int attrCount = entity.getAttributes() != null ? entity.getAttributes().size() : 0;
            double attrSpan = (attrCount <= 1) ? 0 : (attrCount - 1) * 42.0;
            return Math.max(20, (attrSpan + 36) / 2.0); // 36 is ellipse height (18*2)
        }
    }

    private double measureTextWidth(String text, double fontSize) {
        return text.length() * fontSize * 0.55;
    }

    public Pane getCanvas() {
        return canvas;
    }
}
