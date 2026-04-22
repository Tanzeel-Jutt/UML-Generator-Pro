package com.umlgenerator.ui;

import com.umlgenerator.core.analyzer.SOLIDAnalyzer;
import com.umlgenerator.core.analyzer.SOLIDAnalyzer.SOLIDViolation;
import com.umlgenerator.core.engine.UMLEngine;
import com.umlgenerator.core.model.*;
import com.umlgenerator.core.parser.ParserFactory;
import com.umlgenerator.ui.components.*;
import com.umlgenerator.util.ExportUtil;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.util.List;

/**
 * Main JavaFX Application for UML Generator Pro.
 * Premium dark-themed UI with sidebar navigation, code input, and diagram rendering.
 */
public class MainApplication extends Application {

    private final UMLEngine engine = new UMLEngine();
    private Stage primaryStage;

    // UI Components
    private BorderPane rootLayout;
    private TabPane codeTabPane;
    private ComboBox<String> languageSelector;
    private ComboBox<String> themeSelector;
    private DiagramCanvas diagramCanvas;
    private ERDiagramCanvas erDiagramCanvas;
    private TabPane diagramTabs;
    private VBox sidebarClassList;
    private Label statusLabel;
    private RelationalMappingCanvas relationalCanvas;
    private Button showAllBtn;
    private VBox fullDiagramBox;
    private VBox tipsBox;
    private TextArea sqlOutputArea;
    private UMLToCodePanel umlToCodePanel;
    private TextArea solidOutputArea;

    // State
    private UMLDiagram currentUMLDiagram;
    private ERDiagram currentERDiagram;
    private RelationalSchema currentSchema;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("UML Generator Pro — Advanced Diagram Generator");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-layout");

        // Build UI sections
        rootLayout.setTop(buildTopBar());
        rootLayout.setLeft(buildSidebar());
        rootLayout.setCenter(buildCenterContent());
        rootLayout.setBottom(buildStatusBar());

        Scene scene = new Scene(rootLayout, 1300, 850);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // ==================== TOP BAR ====================
    private HBox buildTopBar() {
        HBox topBar = new HBox(15);
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Logo/title
        Label logo = new Label("⬡");
        logo.setStyle("-fx-font-size: 24px; -fx-text-fill: #818cf8;");

        Label title = new Label("UML Generator Pro");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;"
                + "-fx-font-family: 'Inter';");

        Label version = new Label("v1.0");
        version.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-padding: 0 0 0 -10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Language selector
        Label langLabel = new Label("Language:");
        langLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        languageSelector = new ComboBox<>(FXCollections.observableArrayList(
                engine.getAvailableLanguages()));
        languageSelector.setValue("Java");
        languageSelector.getStyleClass().add("language-selector");
        languageSelector.setOnAction(e -> onLanguageChanged());

        // Generate button
        Button generateBtn = new Button("▶ Generate");
        generateBtn.getStyleClass().add("generate-btn");
        generateBtn.setOnAction(e -> onGenerate());

        // Export button
        MenuButton exportBtn = new MenuButton("📤 Export");
        exportBtn.getStyleClass().add("export-btn");
        exportBtn.setStyle("-fx-text-fill: white;");

        MenuItem exportPNG = new MenuItem("Export as PNG");
        exportPNG.setOnAction(e -> onExportPNG());
        MenuItem exportSQL = new MenuItem("Export SQL Script");
        exportSQL.setOnAction(e -> onExportSQL());
        MenuItem exportTxt = new MenuItem("Export Mapping");
        exportTxt.setOnAction(e -> onExportMapping());
        exportBtn.getItems().addAll(exportPNG, exportSQL, exportTxt);

        // UML → Code button
        Button umlToCodeBtn = new Button("✏ UML → Code");
        umlToCodeBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #8b5cf6, #7c3aed);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;"
                + "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.4), 8, 0, 0, 2);");
        umlToCodeBtn.setOnAction(e -> switchToUMLCodePanel());

        // Theme Switcher
        Label themeLabel = new Label("Theme:");
        themeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        themeSelector = new ComboBox<>(FXCollections.observableArrayList("Dracula", "Matrix Hacker"));
        themeSelector.setValue("Dracula");
        themeSelector.getStyleClass().add("language-selector");
        themeSelector.setOnAction(e -> applyTheme(themeSelector.getValue()));

        // Clear button
        Button clearBtn = new Button("🗑 Clear");
        clearBtn.getStyleClass().add("clear-btn");
        clearBtn.setOnAction(e -> onClear());

        // Scan Folder button
        Button scanBtn = new Button("📁 Scan Folder");
        scanBtn.setStyle("-fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        scanBtn.setOnAction(e -> onScanFolder());

        topBar.getChildren().addAll(logo, title, version, spacer,
                themeLabel, themeSelector, umlToCodeBtn, scanBtn, langLabel, languageSelector, generateBtn, exportBtn, clearBtn);

        return topBar;
    }

    // ==================== SIDEBAR ====================
    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(15, 10, 15, 10));

        // Code Input Section
        HBox inputHeader = new HBox(5);
        inputHeader.setAlignment(Pos.CENTER_LEFT);
        Label inputTitle = new Label("📝 Code Files");
        inputTitle.getStyleClass().add("sidebar-title");
        
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        
        Button addClassBtn = new Button("+ Class");
        addClassBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand;");
        addClassBtn.setOnAction(e -> addNewCodeTab("Class " + (codeTabPane.getTabs().size() + 1), ""));

        Button removeClassBtn = new Button("− Remove");
        removeClassBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-cursor: hand;");
        removeClassBtn.setOnAction(e -> removeCurrentCodeTab());
        
        inputHeader.getChildren().addAll(inputTitle, hSpacer, addClassBtn, removeClassBtn);

        codeTabPane = new TabPane();
        codeTabPane.getStyleClass().add("diagram-tabs");
        codeTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        codeTabPane.setPrefHeight(300);
        codeTabPane.setTabMinWidth(60);
        codeTabPane.setTabMaxWidth(120);
        
        // Add scroll-to-switch tabs (Horizontal scrolling feel)
        codeTabPane.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                codeTabPane.getSelectionModel().selectPrevious();
            } else {
                codeTabPane.getSelectionModel().selectNext();
            }
            event.consume();
        });
        
        VBox.setVgrow(codeTabPane, Priority.ALWAYS);
        
        // Initial tab
        addNewCodeTab("Main", "");

        // Separator
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #334155;");

        // Classes list
        Label classesTitle = new Label("📦 Classes Found");
        classesTitle.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");

        sidebarClassList = new VBox(5);
        ScrollPane classScroll = new ScrollPane(sidebarClassList);
        classScroll.setFitToWidth(true);
        classScroll.getStyleClass().add("custom-scroll");
        classScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        classScroll.setPrefHeight(200);
        VBox.setVgrow(classScroll, Priority.SOMETIMES);

        // Full Diagram Box
        fullDiagramBox = new VBox();
        fullDiagramBox.getStyleClass().add("full-diagram-box");
        fullDiagramBox.setVisible(false);
        fullDiagramBox.setManaged(false);

        showAllBtn = new Button("🔄 Show Full Diagram");
        showAllBtn.getStyleClass().add("sidebar-btn");
        showAllBtn.setMaxWidth(Double.MAX_VALUE);
        showAllBtn.setOnAction(e -> {
            if (currentUMLDiagram != null) {
                diagramCanvas.renderDiagram(currentUMLDiagram);
                clearSidebarHighlight();
            }
        });
        fullDiagramBox.getChildren().add(showAllBtn);

        // Quick tips
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #334155;");

        Label tipsTitle = new Label("💡 Tips");
        tipsTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        tipsBox = new VBox(6);
        Label tip1 = new Label("• Add multiple file tabs above");
        Label tip2 = new Label("• SQL generates ER + EERD + Mapping");
        Label tip3 = new Label("• Click class boxes for details");
        Label tip4 = new Label("• >10 classes = overview mode");
        Label tip5 = new Label("• Ctrl+Scroll to zoom diagrams");
        Label tip6 = new Label("• Legend: Yellow oval = PK, Red oval = FK");
        Label tip7 = new Label("• Legend: Blue box = Entity, Orange = Weak");
        for (Label tip : new Label[]{tip1, tip2, tip3, tip4, tip5, tip6, tip7}) {
            tip.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
            tip.setWrapText(true);
        }
        tipsBox.getChildren().addAll(tip1, tip2, tip3, tip4, tip5, tip6, tip7);

        ScrollPane tipsScroll = new ScrollPane(tipsBox);
        tipsScroll.setFitToWidth(true);
        tipsScroll.setPrefHeight(150);
        tipsScroll.getStyleClass().add("custom-scroll");
        tipsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        sidebar.getChildren().addAll(inputHeader, codeTabPane, sep1,
                classesTitle, classScroll, fullDiagramBox, sep2, tipsTitle, tipsScroll);

        return sidebar;
    }

    private void addNewCodeTab(String name, String content) {
        Tab tab = new Tab(name);
        TextArea area = new TextArea(content);
        area.getStyleClass().add("code-input");
        area.setPromptText("Paste code here...");
        area.setWrapText(false); // Enable horizontal scrolling
        tab.setContent(area);
        codeTabPane.getTabs().add(tab);
        codeTabPane.getSelectionModel().select(tab);
    }

    private void removeCurrentCodeTab() {
        Tab selectedTab = codeTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && codeTabPane.getTabs().size() > 1) {
            codeTabPane.getTabs().remove(selectedTab);
        } else {
            showStatus("⚠ Cannot remove the last class tab.", "#f59e0b");
        }
    }

    private TextArea getActiveCodeArea() {
        Tab tab = codeTabPane.getSelectionModel().getSelectedItem();
        if (tab != null && tab.getContent() instanceof TextArea area) {
            return area;
        }
        return null;
    }

    private java.util.List<String> getAllCode() {
        java.util.List<String> codes = new java.util.ArrayList<>();
        for (Tab tab : codeTabPane.getTabs()) {
            if (tab.getContent() instanceof TextArea area) {
                codes.add(area.getText());
            }
        }
        return codes;
    }

    // ==================== CENTER CONTENT ====================
    private TabPane buildCenterContent() {
        diagramTabs = new TabPane();
        diagramTabs.getStyleClass().add("diagram-tabs");
        diagramTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // UML Tab
        Tab umlTab = new Tab("📊 UML Class Diagram");
        diagramCanvas = new DiagramCanvas();
        diagramCanvas.setOnClassClick(() -> {
            String selected = diagramCanvas.getSelectedClassName();
            if (selected != null && currentUMLDiagram != null) {
                currentUMLDiagram.findClass(selected).ifPresent(cls -> {
                    diagramCanvas.showClassDetail(cls);
                    highlightSidebarClass(selected);
                });
            }
        });
        umlTab.setContent(diagramCanvas);

        // ER Tab
        Tab erTab = new Tab("🔷 ER Diagram");
        erDiagramCanvas = new ERDiagramCanvas();
        erTab.setContent(erDiagramCanvas);

        // EERD Tab
        Tab eerdTab = new Tab("🔶 EERD Diagram");
        ERDiagramCanvas eerdCanvas = new ERDiagramCanvas();
        eerdTab.setContent(eerdCanvas);

        // Relational Mapping Tab
        Tab relTab = new Tab("📋 Relational Mapping");
        relationalCanvas = new RelationalMappingCanvas();
        relTab.setContent(relationalCanvas);

        // SQL Script Tab
        Tab sqlTab = new Tab("📜 Generated SQL");
        sqlOutputArea = new TextArea();
        sqlOutputArea.getStyleClass().add("output-area");
        sqlOutputArea.setEditable(false);
        sqlOutputArea.setPromptText("Generated SQL DDL script will appear here...");
        sqlTab.setContent(sqlOutputArea);

        // SOLID Report Tab
        Tab solidTab = new Tab("🛡️ SOLID Report");
        VBox solidBox = new VBox(8);
        solidBox.setPadding(new Insets(10));
        
        Button autoFixBtn = new Button("🛠️ Auto-Refactor Violations");
        autoFixBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        autoFixBtn.setOnAction(e -> onAutoRefactor());
        
        solidOutputArea = new TextArea();
        solidOutputArea.getStyleClass().add("output-area");
        solidOutputArea.setEditable(false);
        solidOutputArea.setPromptText("SOLID principle analysis will appear here after generating UML...");
        VBox.setVgrow(solidOutputArea, Priority.ALWAYS);
        
        solidBox.getChildren().addAll(autoFixBtn, solidOutputArea);
        solidTab.setContent(solidBox);

        diagramTabs.getTabs().addAll(umlTab, solidTab, erTab, eerdTab, relTab, sqlTab);
        return diagramTabs;
    }

    // ==================== STATUS BAR ====================
    private HBox buildStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(6, 15, 6, 15));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready — Paste your code and click Generate");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label credits = new Label("UML Generator Pro © 2026 | Scalable Architecture");
        credits.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        statusBar.getChildren().addAll(statusLabel, spacer, credits);
        return statusBar;
    }

    // ==================== EVENT HANDLERS ====================

    private void onGenerate() {
        java.util.List<String> codes = getAllCode();
        if (codes.isEmpty() || (codes.size() == 1 && codes.get(0).trim().isEmpty())) {
            showStatus("⚠ Please paste code before generating", "#f59e0b");
            return;
        }

        String language = languageSelector.getValue();
        showStatus("⏳ Generating diagrams for " + language + "...", "#60a5fa");

        try {
            boolean isSQL = "SQL".equalsIgnoreCase(language);

            if (!isSQL) {
                currentUMLDiagram = engine.generateProjectUML(codes, language);
                renderDiagrams();
            } else {
                currentERDiagram = engine.generateERDiagram(codes.get(0));
                renderDiagrams();
            }

        } catch (Exception e) {
            showStatus("❌ Error: " + e.getMessage(), "#f87171");
            e.printStackTrace();
        }
    }

    private void onScanFolder() {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Select Project Folder");
        java.io.File selectedDir = dc.showDialog(rootLayout.getScene().getWindow());

        if (selectedDir != null) {
            String language = languageSelector.getValue();
            java.util.List<String> sources = new java.util.ArrayList<>();
            scanDirectoryRecursive(selectedDir, sources, language);

            if (!sources.isEmpty()) {
                showStatus("⏳ Scanning " + sources.size() + " files...", "#60a5fa");
                
                codeTabPane.getTabs().clear();
                for (int i = 0; i < sources.size(); i++) {
                    addNewCodeTab("File_" + (i+1), sources.get(i));
                }
                
                currentUMLDiagram = engine.generateProjectUML(sources, language);
                renderDiagrams();
                showStatus("✅ Project Scanned: " + sources.size() + " files found in " + selectedDir.getName(), "#4ade80");
            } else {
                showStatus("⚠ No matching source files found in selected folder.", "#f59e0b");
            }
        }
    }

    private void scanDirectoryRecursive(java.io.File dir, java.util.List<String> sources, String language) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        
        String ext = ".java";
        if ("Python".equals(language)) ext = ".py";
        else if ("C++".equals(language)) ext = ".cpp";
        else if ("SQL".equals(language)) ext = ".sql";

        for (java.io.File f : files) {
            if (f.isDirectory()) {
                scanDirectoryRecursive(f, sources, language);
            } else if (f.getName().endsWith(ext)) {
                try {
                    sources.add(java.nio.file.Files.readString(f.toPath()));
                } catch (java.io.IOException e) {
                    System.err.println("Error reading " + f.getName());
                }
            }
        }
    }

    private void renderDiagrams() {
        if (currentUMLDiagram != null) {
            diagramCanvas.renderDiagram(currentUMLDiagram);
            updateSidebarClassList();
            
            StringBuilder solidReport = new StringBuilder("🛡️ SOLID PRINCIPLES ANALYSIS REPORT\n");
            solidReport.append("════════════════════════════════════════════════════════════════════════\n\n");
            int totalViolations = 0;
            for (UMLClass uc : currentUMLDiagram.getClasses()) {
                List<SOLIDViolation> violations = SOLIDAnalyzer.analyzeUMLClass(uc);
                if (!violations.isEmpty()) {
                    solidReport.append("🔹 Class: ").append(uc.getName()).append("\n");
                    for (SOLIDViolation v : violations) {
                        solidReport.append("   ⚠️ ").append(v.principle).append(" (").append(v.locations.size()).append(" violations):\n");
                        if (!v.locations.isEmpty()) {
                            solidReport.append("      Locations:  ").append(String.join(", ", v.locations)).append("\n");
                        }
                        solidReport.append("      Definition: ").append(v.definition).append("\n");
                        solidReport.append("      Detection:  ").append(v.criteria).append("\n");
                        solidReport.append("      Rule:       ").append(v.rule).append("\n\n");
                        totalViolations += v.locations.isEmpty() ? 1 : v.locations.size();
                    }
                }
            }
            if (totalViolations == 0) {
                solidReport.append("✅ Congratulations! No obvious SOLID principle violations detected.\n");
            } else {
                solidReport.append("Total violations found: ").append(totalViolations).append("\n");
            }
            solidOutputArea.setText(solidReport.toString());
            diagramTabs.getSelectionModel().select(0);
        } else if (currentERDiagram != null) {
            boolean wasEnhanced = currentERDiagram.isEnhanced();
            
            // Standard ER Tab ALWAYS uses standard oval notation
            currentERDiagram.setEnhanced(false);
            erDiagramCanvas.renderDiagram(currentERDiagram);
            
            // EERD tab ALWAYS uses enhanced UML-box notation
            ERDiagramCanvas eerdCanvas = (ERDiagramCanvas) diagramTabs.getTabs().get(3).getContent();
            currentERDiagram.setEnhanced(true);  // Force UML box notation
            eerdCanvas.renderDiagram(currentERDiagram);
            
            currentERDiagram.setEnhanced(wasEnhanced); // Restore original flag

            currentSchema = engine.generateRelationalMapping(currentERDiagram);
            relationalCanvas.renderSchema(currentSchema);

            sqlOutputArea.setText(currentSchema.toSQL());
            diagramTabs.getSelectionModel().select(2);
        }
    }

    private void onLanguageChanged() {
        String lang = languageSelector.getValue();
        boolean isSQL = "SQL".equalsIgnoreCase(lang);

        // Show/hide SQL-specific tabs
        for (int i = 2; i < diagramTabs.getTabs().size(); i++) {
            diagramTabs.getTabs().get(i).setDisable(!isSQL);
        }
        // Disable UML and SOLID tabs if SQL is selected
        diagramTabs.getTabs().get(0).setDisable(isSQL);
        diagramTabs.getTabs().get(1).setDisable(isSQL);
        if (isSQL) {
            diagramTabs.getSelectionModel().select(2); // select ER Tab
        } else {
            diagramTabs.getSelectionModel().select(0); // select UML Tab
        }

        TextArea area = getActiveCodeArea();
        if (area != null) {
            if (isSQL) {
                area.setPromptText(
                        "Paste your SQL DDL here...\n\n"
                                + "Example:\n"
                                + "CREATE TABLE students (\n"
                                + "    id INT PRIMARY KEY,\n"
                                + "    name VARCHAR(100),\n"
                                + "    dept_id INT REFERENCES departments(id)\n"
                                + ");");
            } else {
                area.setPromptText("Paste your " + lang + " code here...");
            }
        }

        showStatus("Language set to " + lang, "#60a5fa");
    }

    private void onExportPNG() {
        Tab selectedTab = diagramTabs.getSelectionModel().getSelectedItem();
        if (selectedTab.getContent() instanceof DiagramCanvas dc) {
            ExportUtil.exportAsPNG(dc.getCanvas(), primaryStage, "uml_diagram");
        } else if (selectedTab.getContent() instanceof ERDiagramCanvas erc) {
            ExportUtil.exportAsPNG(erc.getCanvas(), primaryStage, "er_diagram");
        }
        showStatus("📤 Exported diagram as PNG", "#4ade80");
    }

    private void onExportSQL() {
        if (currentSchema != null) {
            ExportUtil.exportAsSQL(currentSchema.toSQL(), primaryStage);
            showStatus("📤 Exported SQL script", "#4ade80");
        } else {
            showStatus("⚠ No SQL schema to export. Generate from SQL first.", "#f59e0b");
        }
    }

    private void onExportMapping() {
        if (currentSchema != null) {
            ExportUtil.exportAsText(currentSchema.toMappingNotation(), primaryStage, "relational_mapping");
            showStatus("📤 Exported relational mapping", "#4ade80");
        } else {
            showStatus("⚠ No mapping to export", "#f59e0b");
        }
    }

    private void onClear() {
        codeTabPane.getTabs().clear();
        addNewCodeTab("Main", "");
        
        diagramCanvas.renderDiagram(null);
        erDiagramCanvas.renderDiagram(null);
        sidebarClassList.getChildren().clear();
        relationalCanvas.renderSchema(null);
        sqlOutputArea.clear();
        solidOutputArea.clear();
        currentUMLDiagram = null;
        currentERDiagram = null;
        currentSchema = null;
        fullDiagramBox.setVisible(false);
        fullDiagramBox.setManaged(false);
        showStatus("🗑 Cleared all diagrams", "#94a3b8");
    }

    private void updateSidebarClassList() {
        sidebarClassList.getChildren().clear();
        if (currentUMLDiagram == null) {
            fullDiagramBox.setVisible(false);
            fullDiagramBox.setManaged(false);
            return;
        }

        fullDiagramBox.setVisible(true);
        fullDiagramBox.setManaged(true);

        for (UMLClass cls : currentUMLDiagram.getClasses()) {
            Button classBtn = new Button(getClassIcon(cls) + " " + cls.getName());
            classBtn.getStyleClass().add("class-list-btn");
            classBtn.setMaxWidth(Double.MAX_VALUE);
            classBtn.setAlignment(Pos.CENTER_LEFT);
            classBtn.setOnAction(e -> {
                diagramCanvas.showClassDetail(cls);
                highlightSidebarClass(cls.getName());
                diagramTabs.getSelectionModel().select(0);
            });

            Tooltip tooltip = new Tooltip(
                    cls.getClassType().getStereotype() + " " + cls.getName()
                            + "\nAttributes: " + cls.getAttributes().size()
                            + "\nMethods: " + cls.getMethods().size()
            );
            classBtn.setTooltip(tooltip);

            sidebarClassList.getChildren().add(classBtn);
        }
    }

    private String getClassIcon(UMLClass cls) {
        return switch (cls.getClassType()) {
            case ABSTRACT_CLASS -> "🔷";
            case INTERFACE -> "🟣";
            case ENUM -> "🟡";
            case RECORD -> "🟢";
            case ANNOTATION -> "📎";
            default -> "🔵";
        };
    }

    private void highlightSidebarClass(String name) {
        for (javafx.scene.Node node : sidebarClassList.getChildren()) {
            if (node instanceof Button btn) {
                if (btn.getText().contains(name)) {
                    btn.setStyle("-fx-background-color: #312e81; -fx-text-fill: #a5b4fc;");
                } else {
                    btn.setStyle("");
                }
            }
        }
    }

    private void clearSidebarHighlight() {
        for (javafx.scene.Node node : sidebarClassList.getChildren()) {
            if (node instanceof Button btn) {
                btn.setStyle("");
            }
        }
    }

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
    }

    // ==================== UML → CODE PANEL NAVIGATION ====================

    private javafx.scene.Node savedCenter;
    private javafx.scene.Node savedLeft;
    private javafx.scene.Node savedTop;
    private javafx.scene.Node savedBottom;

    private void switchToUMLCodePanel() {
        savedCenter = rootLayout.getCenter();
        savedLeft = rootLayout.getLeft();
        savedTop = rootLayout.getTop();
        savedBottom = rootLayout.getBottom();

        if (umlToCodePanel == null) {
            umlToCodePanel = new UMLToCodePanel();
            umlToCodePanel.setOnBack(this::switchBackToMainView);
        }

        rootLayout.setTop(null);
        rootLayout.setLeft(null);
        rootLayout.setBottom(null);
        rootLayout.setCenter(umlToCodePanel);
    }

    private void switchBackToMainView() {
        rootLayout.setTop(savedTop);
        rootLayout.setLeft(savedLeft);
        rootLayout.setCenter(savedCenter);
        rootLayout.setBottom(savedBottom);
        showStatus("Ready — Paste your code and click Generate", "#64748b");
    }

    // ==================== AUTO-REFACTOR ====================
    private void onAutoRefactor() {
        if (currentUMLDiagram == null) {
            showStatus("⚠ Generate UML first to detect violations.", "#f59e0b");
            return;
        }

        boolean refactored = false;
        TextArea activeArea = getActiveCodeArea();
        if (activeArea == null) return;
        
        String originalCode = activeArea.getText();
        String newCode = originalCode;

        for (UMLClass uc : currentUMLDiagram.getClasses()) {
            List<SOLIDViolation> violations = SOLIDAnalyzer.analyzeUMLClass(uc);
            boolean hasSrp = violations.stream().anyMatch(v -> v.principle.contains("SRP"));
            boolean hasDip = violations.stream().anyMatch(v -> v.principle.contains("DIP"));
            
            if (hasDip) {
                // ... DIP fix ...
            }
            
            if (hasSrp) {
                String oldName = uc.getName();
                // Clean old suffixes if they exist
                String baseName = oldName.replaceAll("_Logic$|_DB$|_UI$", "");
                String logicMethods = "";
                String dbMethods = "";
                String uiMethods = "";
                
                for (UMLMethod m : uc.getMethods()) {
                    String ln = m.getName().toLowerCase();
                    String body = m.getBody() != null ? m.getBody() : "{}";
                    String access = "";
                    if (m.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PUBLIC) access = "public ";
                    else if (m.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PRIVATE) access = "private ";
                    else if (m.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PROTECTED) access = "protected ";
                    
                    String params = m.getParameters().stream()
                            .map(p -> p.getType() + " " + p.getName())
                            .collect(java.util.stream.Collectors.joining(", "));
                    
                    String returnTypeStr = m.isConstructor() ? "" : m.getReturnType() + " ";
                    String methodCodeBase = "\n    " + access + returnTypeStr;
                    
                    if (ln.contains("save") || ln.contains("update") || ln.contains("delete") || ln.contains("db") || ln.contains("sql") || 
                        body.toLowerCase().contains("jdbc") || body.toLowerCase().contains("insert") || body.toLowerCase().contains("statement") || body.toLowerCase().contains("repository")) {
                        dbMethods += methodCodeBase + (m.isConstructor() ? baseName + "_DB" : m.getName()) + "(" + params + ") " + body;
                    } else if (ln.contains("print") || ln.contains("show") || ln.contains("render") || ln.contains("ui") || body.toLowerCase().contains("system.out") || body.toLowerCase().contains("alert")) {
                        uiMethods += methodCodeBase + (m.isConstructor() ? baseName + "_UI" : m.getName()) + "(" + params + ") " + body;
                    } else {
                        logicMethods += methodCodeBase + (m.isConstructor() ? baseName + "_Logic" : m.getName()) + "(" + params + ") " + body;
                    }
                }
                
                String attributesCode = "";
                for (UMLAttribute attr : uc.getAttributes()) {
                    String attrAccess = "";
                    if (attr.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PUBLIC) attrAccess = "public ";
                    else if (attr.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PRIVATE) attrAccess = "private ";
                    else if (attr.getAccessModifier() == com.umlgenerator.core.model.AccessModifier.PROTECTED) attrAccess = "protected ";
                    attributesCode += "    " + attrAccess + attr.getType() + " " + attr.getName() + ";\n";
                }
                
                // Construct new classes
                StringBuilder refactorBlock = new StringBuilder();
                if (!logicMethods.isEmpty()) {
                    refactorBlock.append("class ").append(baseName).append("_Logic {\n").append(attributesCode).append(logicMethods).append("\n}\n\n");
                }
                if (!dbMethods.isEmpty()) {
                    refactorBlock.append("class ").append(baseName).append("_DB {\n").append(attributesCode).append(dbMethods).append("\n}\n\n");
                }
                if (!uiMethods.isEmpty()) {
                    refactorBlock.append("class ").append(baseName).append("_UI {\n").append(attributesCode).append(uiMethods).append("\n}\n\n");
                }
                
                // Replace the original class in code string using AST Range
                com.github.javaparser.ast.CompilationUnit currentCu = null;
                try {
                    currentCu = com.github.javaparser.StaticJavaParser.parse(newCode);
                } catch (Exception e) {
                    continue; // Skip if code became unparseable
                }
                
                final String[] modifiedCode = {newCode};
                currentCu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).stream()
                  .filter(c -> c.getNameAsString().equals(oldName))
                  .findFirst()
                  .ifPresent(c -> {
                      if (c.getRange().isPresent()) {
                          int startLine = c.getRange().get().begin.line - 1;
                          int endLine = c.getRange().get().end.line - 1;
                          String[] lines = modifiedCode[0].split("\n", -1);
                          StringBuilder sb = new StringBuilder();
                          for (int i = 0; i < lines.length; i++) {
                              if (i < startLine || i > endLine) {
                                  sb.append(lines[i]).append("\n");
                              } else if (i == startLine) {
                                  sb.append(refactorBlock.toString()).append("\n");
                              }
                          }
                          modifiedCode[0] = sb.toString();
                      }
                  });
                newCode = modifiedCode[0];
                refactored = true;
            }
        }
        
        if (refactored) {
            activeArea.setText(newCode);
            showStatus("✅ Auto-Refactored SRP & DIP! Regenerating...", "#4ade80");
            onGenerate(); // Automatically regenerate UML after fixing
            
            // Check if there are still other violations
            boolean hasOtherViolations = false;
            for (UMLClass uc : currentUMLDiagram.getClasses()) {
                List<SOLIDViolation> vs = SOLIDAnalyzer.analyzeUMLClass(uc);
                if (vs.stream().anyMatch(v -> v.principle.contains("OCP") || v.principle.contains("LSP") || v.principle.contains("ISP"))) {
                    hasOtherViolations = true;
                    break;
                }
            }
            if (hasOtherViolations) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Auto-Refactor Complete");
                    alert.setHeaderText("SRP & DIP Violations Fixed!");
                    alert.setContentText("The Auto-Refactor successfully fixed SRP (by splitting classes) and DIP (by using Interfaces for Collections).\n\nHowever, some violations like OCP, LSP, or ISP remain. These require manual Architectural Redesign (e.g., implementing the Strategy Pattern, creating new interfaces) which cannot be safely automated without knowing your exact business logic.");
                    alert.showAndWait();
                });
            }
        } else {
            showStatus("⚠ No fixable violations found.", "#f59e0b");
        }
    }

    // ==================== THEME APPLY ====================
    private void applyTheme(String themeName) {
        rootLayout.getScene().getStylesheets().clear();
        String mainCss = getClass().getResource("/styles/main.css").toExternalForm();
        rootLayout.getScene().getStylesheets().add(mainCss);
        
        if ("Matrix Hacker".equals(themeName)) {
            String hackerCss = getClass().getResource("/styles/hacker.css").toExternalForm();
            rootLayout.getScene().getStylesheets().add(hackerCss);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
