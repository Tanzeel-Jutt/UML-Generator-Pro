package com.umlgenerator.ui.components;

import com.umlgenerator.core.model.*;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual renderer for Relational Schema Mapping.
 * Implements "Zero-Over-Box" Orthogonal Routing via right-side gutter.
 */
public class RelationalMappingCanvas extends ScrollPane {
    private Pane canvas;
    private RelationalSchema schema;
    private Map<String, VBox> tableNodes = new HashMap<>();
    private Map<String, Map<String, StackPane>> columnNodes = new HashMap<>();
    private int arrowCounter = 0;

    public RelationalMappingCanvas() {
        this.canvas = new Pane();
        this.canvas.getStyleClass().add("relational-canvas");
        this.canvas.setStyle("-fx-background-color: #0f172a;");
        
        Group zoomGroup = new Group(canvas);
        
        setContent(zoomGroup);
        setPannable(true);
        setFitToHeight(false);
        setFitToWidth(false);
        setStyle("-fx-background-color: #0f172a; -fx-border-color: transparent;");
        
        // Add Zoom via Mouse Wheel
        addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (event.isControlDown() || event.isAltDown()) { // Standard modifier for zooming
                event.consume();
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                double currentScale = canvas.getScaleX();
                double newScale = Math.max(0.2, Math.min(3.0, currentScale * zoomFactor));
                canvas.setScaleX(newScale);
                canvas.setScaleY(newScale);
            }
        });
    }

    public void renderSchema(RelationalSchema schema) {
        this.schema = schema;
        canvas.getChildren().clear();
        tableNodes.clear();
        columnNodes.clear();

        if (schema == null || schema.getTables().isEmpty()) return;

        double currentY = 50;
        double startX = 50;
        double tableSpacing = 140; // Increased spacing for better gap routing

        for (RelationalTable table : schema.getTables()) {
            VBox tableContainer = new VBox(6);
            tableContainer.setLayoutX(startX);
            tableContainer.setLayoutY(currentY);

            // Table Name
            Label nameLabel = new Label(table.getName().toUpperCase());
            nameLabel.setFont(Font.font("Inter", FontWeight.BOLD, 13));
            nameLabel.setTextFill(Color.web("#94a3b8"));
            tableContainer.getChildren().add(nameLabel);

            // Columns Row
            HBox columnsRow = new HBox();
            columnsRow.setStyle("-fx-border-color: #334155; -fx-border-width: 2; -fx-background-color: #1e293b; -fx-background-radius: 4; -fx-border-radius: 4;");
            
            Map<String, StackPane> colMap = new HashMap<>();
            for (int i = 0; i < table.getColumns().size(); i++) {
                RelationalColumn col = table.getColumns().get(i);
                StackPane cell = createColumnCell(col, i == table.getColumns().size() - 1);
                columnsRow.getChildren().add(cell);
                colMap.put(col.getName(), cell);
            }
            tableContainer.getChildren().add(columnsRow);
            
            canvas.getChildren().add(tableContainer);
            tableNodes.put(table.getName(), tableContainer);
            columnNodes.put(table.getName(), colMap);

            currentY += tableSpacing;
        }

        Platform.runLater(() -> {
            canvas.applyCss();
            canvas.layout();
            drawForeignKeyArrows();
        });
        
        canvas.setPrefWidth(2500);
        canvas.setPrefHeight(currentY + 400);
    }

    private StackPane createColumnCell(RelationalColumn col, boolean isLast) {
        StackPane cell = new StackPane();
        cell.setPadding(new Insets(10, 18, 10, 18));
        cell.setMinWidth(110);
        if (!isLast) {
            cell.setStyle("-fx-border-color: transparent #334155 transparent transparent; -fx-border-width: 0 1 0 0;");
        }

        Label label = new Label(col.getName());
        label.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 12));
        
        if (col.isPrimaryKey()) {
            label.setTextFill(Color.web("#fbbf24"));
            label.setUnderline(true);
        } else if (col.isForeignKey()) {
            label.setTextFill(Color.web("#f87171"));
        } else {
            label.setTextFill(Color.web("#e2e8f0"));
        }

        cell.getChildren().add(label);
        return cell;
    }

    private void drawForeignKeyArrows() {
        if (schema == null) return;
        arrowCounter = 0;
        for (RelationalTable table : schema.getTables()) {
            for (RelationalColumn col : table.getColumns()) {
                if (col.isForeignKey() && col.getReferencedTable() != null) {
                    drawArrow(table.getName(), col.getName(), 
                             col.getReferencedTable(), col.getReferencedColumn());
                }
            }
        }
    }

    private static final Color[] ARROW_COLORS = {
        Color.web("#ef4444"), Color.web("#f97316"), Color.web("#eab308"),
        Color.web("#22c55e"), Color.web("#06b6d4"), Color.web("#8b5cf6"),
        Color.web("#ec4899"), Color.web("#3b82f6")
    };

    private void drawArrow(String srcTable, String srcCol, String targetTable, String targetCol) {
        VBox srcContainer = tableNodes.get(srcTable);
        Map<String, StackPane> srcTableCols = columnNodes.get(srcTable);
        VBox targetContainer = tableNodes.get(targetTable);
        Map<String, StackPane> targetTableCols = columnNodes.get(targetTable);

        if (srcContainer == null || srcTableCols == null || targetContainer == null || targetTableCols == null) return;
        
        StackPane srcCell = srcTableCols.get(srcCol);
        StackPane targetCell = targetTableCols.get(targetCol != null ? targetCol : "id");
        if (targetCell == null) targetCell = targetTableCols.values().stream().findFirst().orElse(null);
        if (srcCell == null || targetCell == null) return;

        HBox srcRow = (HBox) srcContainer.getChildren().get(1);
        HBox targetRow = (HBox) targetContainer.getChildren().get(1);

        boolean targetIsAbove = targetContainer.getLayoutY() < srcContainer.getLayoutY();

        // Connection points - exit/enter vertically from cells
        double startX = srcContainer.getLayoutX() + srcRow.getLayoutX() + srcCell.getLayoutX() + srcCell.getWidth() / 2;
        double startY = srcContainer.getLayoutY() + srcRow.getLayoutY() + srcCell.getLayoutY() + (targetIsAbove ? 0 : srcCell.getHeight());

        double targetX = targetContainer.getLayoutX() + targetRow.getLayoutX() + targetCell.getLayoutX() + targetCell.getWidth() / 2;
        double targetY = targetContainer.getLayoutY() + targetRow.getLayoutY() + targetCell.getLayoutY() + (targetIsAbove ? targetCell.getHeight() : 0);

        // Find safe gap Y between adjacent tables (midpoint of the gap)
        double srcGapY;
        double targetGapY;
        
        // Collect all table Y positions for gap calculation
        double[] allTops = tableNodes.values().stream().mapToDouble(VBox::getLayoutY).sorted().toArray();
        double[] allBots = tableNodes.values().stream().mapToDouble(v -> v.getLayoutY() + v.prefHeight(-1)).sorted().toArray();
        
        if (targetIsAbove) {
            // Find gap above source table
            double srcTop = srcContainer.getLayoutY();
            double prevBot = 0;
            for (double b : allBots) { if (b <= srcTop) prevBot = b; }
            srcGapY = prevBot + (srcTop - prevBot) / 2 + (arrowCounter % 5) * 5;
            
            // Find gap below target table
            double tgtBot = targetContainer.getLayoutY() + targetContainer.prefHeight(-1);
            double nextTop = tgtBot + 200;
            for (double t : allTops) { if (t > tgtBot) { nextTop = t; break; } }
            targetGapY = tgtBot + (nextTop - tgtBot) / 2 + (arrowCounter % 5) * 5;
        } else {
            double srcBot = srcContainer.getLayoutY() + srcContainer.prefHeight(-1);
            double nextTop = srcBot + 200;
            for (double t : allTops) { if (t > srcBot) { nextTop = t; break; } }
            srcGapY = srcBot + (nextTop - srcBot) / 2 + (arrowCounter % 5) * 5;
            
            double tgtTop = targetContainer.getLayoutY();
            double prevBot = 0;
            for (double b : allBots) { if (b <= tgtTop) prevBot = b; }
            targetGapY = prevBot + (tgtTop - prevBot) / 2 + (arrowCounter % 5) * 5;
        }

        // Right-side gutter with wide lane spacing
        double maxRight = 0;
        for (VBox box : tableNodes.values()) {
            maxRight = Math.max(maxRight, box.getLayoutX() + box.prefWidth(-1));
        }
        double gutterX = maxRight + 50 + (arrowCounter * 28); // wider lanes for text
        
        Color arrowColor = ARROW_COLORS[arrowCounter % ARROW_COLORS.length];
        arrowCounter++;

        double[][] points = {
            {startX, startY},
            {startX, srcGapY},
            {gutterX, srcGapY},
            {gutterX, targetGapY},
            {targetX, targetGapY},
            {targetX, targetY}
        };

        Path path = buildRoundedPath(points, 12);
        path.setStroke(arrowColor);
        path.setStrokeWidth(1.6);
        path.setOpacity(0.85);
        path.setFill(null);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);

        // Arrow head at target
        double hs = 5;
        Polygon head = targetIsAbove ? 
            new Polygon(targetX, targetY, targetX - hs, targetY + hs*2, targetX + hs, targetY + hs*2) :
            new Polygon(targetX, targetY, targetX - hs, targetY - hs*2, targetX + hs, targetY - hs*2);
        head.setFill(arrowColor);
        head.setOpacity(0.85);

        // Label in the gutter
        Label label = new Label(srcCol + " → " + targetTable);
        label.setFont(Font.font("Inter", FontWeight.BOLD, 10));
        label.setTextFill(arrowColor);
        label.setRotate(90);
        
        // Position it at the midpoint of the gutter segment
        double labelY = (srcGapY + targetGapY) / 2;
        label.setLayoutX(gutterX + 4);
        label.setLayoutY(labelY);
        label.setOpacity(0.85);

        canvas.getChildren().addAll(path, head, label);
    }
    
    private Path buildRoundedPath(double[][] points, double radius) {
        Path path = new Path();
        if (points.length < 2) return path;
        
        path.getElements().add(new MoveTo(points[0][0], points[0][1]));
        
        for (int j = 1; j < points.length - 1; j++) {
            double[] prev = points[j - 1];
            double[] cur = points[j];
            double[] next = points[j + 1];
            
            double dxI = cur[0] - prev[0], dyI = cur[1] - prev[1];
            double dxO = next[0] - cur[0], dyO = next[1] - cur[1];
            double lI = Math.hypot(dxI, dyI), lO = Math.hypot(dxO, dyO);
            
            if (lI < 2 || lO < 2) {
                path.getElements().add(new LineTo(cur[0], cur[1]));
                continue;
            }
            
            double r = Math.min(radius, Math.min(lI / 2, lO / 2));
            double bx = cur[0] - (dxI / lI) * r;
            double by = cur[1] - (dyI / lI) * r;
            double ax = cur[0] + (dxO / lO) * r;
            double ay = cur[1] + (dyO / lO) * r;
            
            path.getElements().add(new LineTo(bx, by));
            path.getElements().add(new QuadCurveTo(cur[0], cur[1], ax, ay));
        }
        
        double[] last = points[points.length - 1];
        path.getElements().add(new LineTo(last[0], last[1]));
        
        return path;
    }
}
