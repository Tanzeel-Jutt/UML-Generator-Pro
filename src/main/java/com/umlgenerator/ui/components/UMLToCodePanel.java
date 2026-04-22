package com.umlgenerator.ui.components;

import com.umlgenerator.core.analyzer.SOLIDAnalyzer;
import com.umlgenerator.core.analyzer.SOLIDAnalyzer.SOLIDViolation;
import com.umlgenerator.core.generator.BoilerplateCodeGenerator;
import com.umlgenerator.core.generator.BoilerplateCodeGenerator.ParsedClass;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel that lets users write UML notation manually in 3 boxes
 * (Class Name, Attributes, Methods) and see generated boilerplate
 * code on the right side in real-time.
 *
 * Supports multiple classes via a tab system.
 * Tips are shown in a dedicated scrollable side panel.
 *
 * This version uses a StackPane as the root to allow "floating" toast
 * notifications that don't shift the layout.
 */
public class UMLToCodePanel extends StackPane {

    private final BoilerplateCodeGenerator generator = new BoilerplateCodeGenerator();

    // Per-class data holder
    private static class ClassEntry {
        TextArea classNameArea;
        TextArea attributesArea;
        TextArea methodsArea;
        Tab tab;
        String tabTitle;

        ClassEntry(String title) { this.tabTitle = title; }
    }

    private final List<ClassEntry> classEntries = new ArrayList<>();
    private TabPane classTabPane;
    private TextArea codeOutputArea;
    private TextArea solidOutputArea;
    private ComboBox<String> langSelector;
    private Label statusLabel;
    private TitledPane tipsPane;
    private BorderPane mainLayout;

    // Callback to go back to main view
    private Runnable onBackHandler;

    public UMLToCodePanel() {
        getStyleClass().add("root-layout");
        setStyle("-fx-background-color: #0f172a;");
        buildUI();
    }

    public void setOnBack(Runnable handler) {
        this.onBackHandler = handler;
    }

    // ──────────────── BUILD UI ────────────────

    private void buildUI() {
        mainLayout = new BorderPane();
        mainLayout.setTop(buildHeader());
        mainLayout.setCenter(buildMainSplit());
        mainLayout.setBottom(buildFooter());

        getChildren().add(mainLayout);
    }

    // ════════════ HEADER ════════════
    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #1e1b4b, #312e81, #1e1b4b);"
                + "-fx-border-color: transparent transparent #334155 transparent;"
                + "-fx-border-width: 0 0 1 0;");

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8;"
                + "-fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;"
                + "-fx-padding: 7 15; -fx-cursor: hand; -fx-font-size: 12px;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backBtn.getStyle().replace("#1e293b", "#334155")));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(backBtn.getStyle().replace("#334155", "#1e293b")));
        backBtn.setOnAction(e -> { if (onBackHandler != null) onBackHandler.run(); });

        Label icon = new Label("✏️");
        icon.setStyle("-fx-font-size: 20px;");

        Label title = new Label("UML → Code Generator");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;"
                + "-fx-font-family: 'Inter';");

        Label subtitle = new Label("Write UML notation → Get boilerplate code");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label langLabel = new Label("Target Language:");
        langLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        langSelector = new ComboBox<>(FXCollections.observableArrayList("Java", "Python", "C++"));
        langSelector.setValue("Java");
        langSelector.getStyleClass().add("language-selector");
        langSelector.setOnAction(e -> regenerateAll());

        Button generateBtn = new Button("⚡ Generate Code");
        generateBtn.getStyleClass().add("generate-btn");
        generateBtn.setOnAction(e -> regenerateAll());

        // Tips toggle button
        Button tipsBtn = new Button("💡 Tips");
        tipsBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #d97706, #b45309);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;"
                + "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        tipsBtn.setOnAction(e -> {
            if (tipsPane != null) tipsPane.setExpanded(!tipsPane.isExpanded());
        });

        header.getChildren().addAll(backBtn, icon, title, subtitle, spacer,
                tipsBtn, langLabel, langSelector, generateBtn);
        return header;
    }

    // ════════════ MAIN SPLIT ════════════
    private SplitPane buildMainSplit() {
        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: #0f172a;");
        mainSplit.setDividerPositions(0.42);

        // Left: class tabs + tips at bottom
        VBox leftContainer = new VBox(0);
        leftContainer.setStyle("-fx-background-color: #0f172a;");

        // ── Class Tabs with Add/Remove ──
        HBox tabControlBar = new HBox(6);
        tabControlBar.setPadding(new Insets(8, 10, 4, 10));
        tabControlBar.setAlignment(Pos.CENTER_LEFT);
        tabControlBar.setStyle("-fx-background-color: #1e293b;");

        Label classLabel = new Label("📦 Classes:");
        classLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button addClassBtn = new Button("+ Add Class");
        addClassBtn.setStyle("-fx-background-color: #065f46; -fx-text-fill: #6ee7b7;"
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12;"
                + "-fx-background-radius: 6; -fx-cursor: hand;");
        addClassBtn.setOnMouseEntered(e -> addClassBtn.setStyle(addClassBtn.getStyle()
                .replace("#065f46", "#047857")));
        addClassBtn.setOnMouseExited(e -> addClassBtn.setStyle(addClassBtn.getStyle()
                .replace("#047857", "#065f46")));
        addClassBtn.setOnAction(e -> addNewClassTab());

        Button removeClassBtn = new Button("− Remove");
        removeClassBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;"
                + "-fx-font-size: 11px; -fx-padding: 4 12;"
                + "-fx-background-radius: 6; -fx-cursor: hand;");
        removeClassBtn.setOnAction(e -> removeCurrentClassTab());

        Region tabSpacer = new Region();
        HBox.setHgrow(tabSpacer, Priority.ALWAYS);

        tabControlBar.getChildren().addAll(classLabel, addClassBtn, removeClassBtn, tabSpacer);

        classTabPane = new TabPane();
        classTabPane.getStyleClass().add("diagram-tabs");
        classTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        classTabPane.setTabMinWidth(60);
        classTabPane.setTabMaxWidth(120);
        
        // Add scroll-to-switch tabs
        classTabPane.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                classTabPane.getSelectionModel().selectPrevious();
            } else {
                classTabPane.getSelectionModel().selectNext();
            }
            event.consume();
        });

        VBox.setVgrow(classTabPane, Priority.ALWAYS);

        // Add first class tab
        addNewClassTab();

        // Tips pane (separate, collapsible, scrollable)
        tipsPane = buildTipsPane();

        leftContainer.getChildren().addAll(tabControlBar, classTabPane, tipsPane);

        // Right: code output
        VBox rightPane = buildRightPane();

        mainSplit.getItems().addAll(leftContainer, rightPane);
        return mainSplit;
    }

    // ──── Create a new class tab ────
    private void addNewClassTab() {
        int idx = classEntries.size() + 1;
        ClassEntry entry = new ClassEntry("Class " + idx);

        // ── BOX 1: Class Name ──
        Label classTitle = new Label("1️⃣  Class Name");
        classTitle.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 13px; -fx-font-weight: bold;");

        entry.classNameArea = new TextArea();
        entry.classNameArea.getStyleClass().add("code-input");
        entry.classNameArea.setPrefHeight(50);
        entry.classNameArea.setMaxHeight(55);
        entry.classNameArea.setPromptText("e.g.  Student\nor    <<abstract>> Animal\nor    <<interface>> Drawable");
        entry.classNameArea.setWrapText(false);
        entry.classNameArea.textProperty().addListener((o, ov, nv) -> {
            // Update tab title dynamically
            String name = nv != null ? nv.trim() : "";
            name = name.replaceAll("(?i)<<\\w+>>|«\\w+»", "").trim();
            if (entry.tab != null) {
                if (!name.isEmpty()) {
                    String first = name.split("\\s+")[0];
                    entry.tab.setText("📄 " + first);
                    entry.tabTitle = first;
                } else {
                    entry.tab.setText("📄 Class " + (classEntries.indexOf(entry) + 1));
                }
            }
            regenerateAll();
        });

        // ── BOX 2: Attributes ──
        Label attrTitle = new Label("2️⃣  Attributes / Fields");
        attrTitle.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");

        entry.attributesArea = new TextArea();
        entry.attributesArea.getStyleClass().add("code-input");
        entry.attributesArea.setPrefHeight(130);
        entry.attributesArea.setPromptText(
                "One attribute per line:\n"
              + "- name: String\n"
              + "+ age: int\n"
              + "# count: int {static}\n"
              + "- MAX_SIZE: int {static, final}");
        entry.attributesArea.setWrapText(false);
        entry.attributesArea.textProperty().addListener((o, ov, nv) -> regenerateAll());
        VBox.setVgrow(entry.attributesArea, Priority.ALWAYS);

        // ── BOX 3: Methods ──
        Label methTitle = new Label("3️⃣  Methods / Constructors");
        methTitle.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");

        entry.methodsArea = new TextArea();
        entry.methodsArea.getStyleClass().add("code-input");
        entry.methodsArea.setPrefHeight(150);
        entry.methodsArea.setPromptText(
                "One method per line:\n"
              + "+ Student(name: String, age: int)\n"
              + "+ getName(): String\n"
              + "- calculateGPA(): double\n"
              + "+ display(): void {static}\n"
              + "+ doWork(): void {abstract}");
        entry.methodsArea.setWrapText(false);
        entry.methodsArea.textProperty().addListener((o, ov, nv) -> regenerateAll());
        VBox.setVgrow(entry.methodsArea, Priority.ALWAYS);

        // Layout
        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: #0f172a;");
        content.getChildren().addAll(classTitle, entry.classNameArea,
                attrTitle, entry.attributesArea,
                methTitle, entry.methodsArea);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: transparent;");

        Tab tab = new Tab("📄 Class " + idx);
        tab.setContent(scroll);
        entry.tab = tab;

        classEntries.add(entry);
        classTabPane.getTabs().add(tab);
        classTabPane.getSelectionModel().select(tab);
    }

    private void removeCurrentClassTab() {
        if (classEntries.size() <= 1) return; // Must keep at least 1
        Tab selected = classTabPane.getSelectionModel().getSelectedItem();
        for (int i = 0; i < classEntries.size(); i++) {
            if (classEntries.get(i).tab == selected) {
                classEntries.remove(i);
                classTabPane.getTabs().remove(selected);
                regenerateAll();
                return;
            }
        }
    }

    // ──── Tips Panel (separate collapsible) ────
    private TitledPane buildTipsPane() {
        VBox tips = new VBox(6);
        tips.setPadding(new Insets(10, 12, 10, 12));
        tips.setStyle("-fx-background-color: #1e293b;");

        String[][] tipData = {
            {"🔑 Access Modifiers",
             "+  →  public        -  →  private\n"
           + "#  →  protected     ~  →  package-private\n"
           + "None → default access"},
            {"📝 Attribute Format",
             "access name: Type\n"
           + "Example:  - name: String\n"
           + "Example:  + age: int"},
            {"⚙️ Method Format",
             "access name(params): ReturnType\n"
           + "Example:  + getName(): String\n"
           + "Example:  + setName(name: String): void"},
            {"🔧 Params with types",
             "+ setData(name: String, id: int): void"},
            {"🏗️ Constructor",
             "Use class name as method name:\n"
           + "Example:  + Student(name: String)\n"
           + "Note: Default constructor is auto-generated!"},
            {"📌 Static members",
             "Cannot draw underline in text, so use:\n"
           + "  {static}  in curly braces\n"
           + "Example:  - count: int {static}\n"
           + "Example:  + getInstance(): MyClass {static}"},
            {"🔒 Final fields",
             "Use  {final}  in curly braces\n"
           + "Example:  - PI: double {final}"},
            {"🔗 Combined modifiers",
             "Use  {static, final}  with comma\n"
           + "Example:  - MAX: int {static, final}"},
            {"📐 Abstract methods",
             "Use  {abstract}  in curly braces\n"
           + "Example:  + draw(): void {abstract}"},
            {"🔷 Abstract class",
             "Write in Class Name box:\n"
           + "  <<abstract>> ClassName"},
            {"🟣 Interface",
             "Write in Class Name box:\n"
           + "  <<interface>> InterfaceName"},
            {"🟡 Enum",
             "Write in Class Name box:\n"
           + "  <<enum>> EnumName"},
            {"📦 Multiple Classes",
             "Click '+ Add Class' to create more class tabs.\n"
           + "All classes are generated together.\n"
           + "Each tab = one class."},
        };

        for (String[] row : tipData) {
            VBox tipCard = new VBox(2);
            tipCard.setPadding(new Insets(6, 8, 6, 8));
            tipCard.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 6;"
                    + "-fx-border-color: #334155; -fx-border-radius: 6; -fx-border-width: 1;");

            Label key = new Label(row[0]);
            key.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: bold;");

            Label val = new Label(row[1]);
            val.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-family: 'Consolas';");
            val.setWrapText(true);

            tipCard.getChildren().addAll(key, val);
            tips.getChildren().add(tipCard);
        }

        ScrollPane tipsScroll = new ScrollPane(tips);
        tipsScroll.setFitToWidth(true);
        tipsScroll.setPrefHeight(200);
        tipsScroll.setMaxHeight(300);
        tipsScroll.setStyle("-fx-background: #1e293b; -fx-background-color: transparent;");

        TitledPane pane = new TitledPane("💡 UML Notation Tips & Guide", tipsScroll);
        pane.setExpanded(false);
        pane.setAnimated(true);
        pane.setStyle("-fx-text-fill: #818cf8; -fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-background-color: #1e293b; -fx-border-color: #334155;"
                + "-fx-border-width: 1 0 0 0;");

        return pane;
    }

    // ──── RIGHT PANE: Code Output ────
    private VBox buildRightPane() {
        VBox right = new VBox(8);
        right.setPadding(new Insets(15));
        right.setStyle("-fx-background-color: #0f172a;");

        TabPane rightTabs = new TabPane();
        rightTabs.getStyleClass().add("diagram-tabs");
        rightTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Code Tab
        Tab codeTab = new Tab("📋 Boilerplate Code");
        VBox codeBox = new VBox(8);
        codeBox.setStyle("-fx-background-color: transparent;");
        codeOutputArea = new TextArea();
        codeOutputArea.getStyleClass().add("output-area");
        codeOutputArea.setEditable(false);
        codeOutputArea.setWrapText(false);
        codeOutputArea.setPromptText("Generated code will appear here...\n\n"
                + "Fill in the UML boxes on the left and code\n"
                + "will be generated automatically!\n\n"
                + "Use '+ Add Class' to create multiple classes.");
        VBox.setVgrow(codeOutputArea, Priority.ALWAYS);

        // Copy button
        Button copyBtn = new Button("📋 Copy to Clipboard");
        copyBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #6366f1, #4f46e5);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;"
                + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> {
            String code = codeOutputArea.getText();
            if (code != null && !code.isEmpty()) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(code);
                clipboard.setContent(content);
                statusLabel.setText("✅ Code copied to clipboard!");
                statusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");
                
                // Show floating toast at the VERY TOP of the entire panel
                showToast("✅ Code copied to clipboard!");
            }
        });
        codeBox.getChildren().addAll(codeOutputArea, copyBtn);
        codeTab.setContent(codeBox);

        // SOLID Tab
        Tab solidTab = new Tab("🛡️ SOLID Report");
        VBox solidBox = new VBox(8);
        solidBox.setStyle("-fx-background-color: transparent;");
        solidOutputArea = new TextArea();
        solidOutputArea.getStyleClass().add("output-area");
        solidOutputArea.setEditable(false);
        solidOutputArea.setWrapText(true);
        solidOutputArea.setPromptText("SOLID principle analysis will appear here...");
        VBox.setVgrow(solidOutputArea, Priority.ALWAYS);
        solidBox.getChildren().add(solidOutputArea);
        solidTab.setContent(solidBox);

        rightTabs.getTabs().addAll(codeTab, solidTab);
        VBox.setVgrow(rightTabs, Priority.ALWAYS);

        right.getChildren().add(rightTabs);
        return right;
    }

    // ════════════ FOOTER ════════════
    private HBox buildFooter() {
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(6, 15, 6, 15));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color: #1e293b;"
                + "-fx-border-color: #334155 transparent transparent transparent;"
                + "-fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Ready — Write UML notation in the boxes and code will generate automatically");
        statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label credits = new Label("UML → Code | Supports: Java, Python, C++ | Multiple Classes");
        credits.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        footer.getChildren().addAll(statusLabel, spacer, credits);
        return footer;
    }

    // ──────────────── LOGIC ────────────────

    private void regenerateAll() {
        String lang = langSelector.getValue();
        StringBuilder allCode = new StringBuilder();
        int totalAttrs = 0;
        int totalMethods = 0;
        int classCount = 0;

        StringBuilder solidReport = new StringBuilder("🛡️ SOLID PRINCIPLES ANALYSIS REPORT\n");
        solidReport.append("════════════════════════════════════════════════════════════════════════\n\n");
        int totalSolidViolations = 0;

        for (ClassEntry entry : classEntries) {
            String classText = entry.classNameArea.getText();
            String attrText = entry.attributesArea.getText();
            String methText = entry.methodsArea.getText();

            if ((classText == null || classText.trim().isEmpty())
                    && (attrText == null || attrText.trim().isEmpty())
                    && (methText == null || methText.trim().isEmpty())) {
                continue; // skip empty tabs
            }

            if (classText == null || classText.trim().isEmpty()) {
                continue; // skip classes without names
            }

            try {
                ParsedClass pc = generator.parse(
                        classText != null ? classText : "",
                        attrText != null ? attrText : "",
                        methText != null ? methText : "");
                String code = generator.generateCode(pc, lang);

                if (allCode.length() > 0) {
                    allCode.append("\n// ════════════════════════════════════════\n\n");
                }
                allCode.append(code);

                // --- SOLID Analysis ---
                List<SOLIDViolation> violations = SOLIDAnalyzer.analyzeParsedClass(pc);
                if (!violations.isEmpty()) {
                    solidReport.append("🔹 Class: ").append(pc.name).append("\n");
                    for (SOLIDViolation v : violations) {
                        solidReport.append("   ⚠️ ").append(v.principle).append(" (").append(v.locations.size()).append(" violations):\n");
                        if (!v.locations.isEmpty()) {
                            solidReport.append("      Locations:  ").append(String.join(", ", v.locations)).append("\n");
                        }
                        solidReport.append("      Definition: ").append(v.definition).append("\n");
                        solidReport.append("      Detection:  ").append(v.criteria).append("\n");
                        solidReport.append("      Rule:       ").append(v.rule).append("\n\n");
                        totalSolidViolations += v.locations.isEmpty() ? 1 : v.locations.size();
                    }
                }

                totalAttrs += pc.attributes.size();
                totalMethods += pc.methods.size();
                classCount++;
            } catch (Exception ex) {
                allCode.append("// ❌ Error in ").append(entry.tabTitle)
                       .append(": ").append(ex.getMessage()).append("\n\n");
            }
        }

        if (classCount == 0) {
            codeOutputArea.clear();
            solidOutputArea.clear();
            statusLabel.setText("Ready — Enter a class name to begin");
            statusLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            return;
        }

        codeOutputArea.setText(allCode.toString());
        
        if (totalSolidViolations == 0) {
            solidReport.append("✅ Congratulations! No obvious SOLID principle violations detected in the manual UML input.\n");
        } else {
            solidReport.append("Total violations found: ").append(totalSolidViolations).append("\n");
        }
        solidOutputArea.setText(solidReport.toString());
        
        statusLabel.setText("✅ Generated " + lang + " | "
                + classCount + " class(es), "
                + totalAttrs + " attributes, "
                + totalMethods + " methods");
        statusLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");
    }

    // ──────────────── TOAST POPUP ────────────────

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color: linear-gradient(to right, #065f46, #047857, #065f46);"
              + "-fx-text-fill: #ecfdf5;"
              + "-fx-font-size: 14px;"
              + "-fx-font-weight: bold;"
              + "-fx-padding: 12 28;"
              + "-fx-background-radius: 10;"
              + "-fx-border-color: #34d399;"
              + "-fx-border-radius: 10;"
              + "-fx-border-width: 1;");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(52, 211, 153, 0.6));
        glow.setRadius(20);
        glow.setSpread(0.25);
        toast.setEffect(glow);

        toast.setOpacity(0);
        toast.setTranslateY(-40);

        // Position at the absolute TOP CENTER of the entire screen/panel
        StackPane overlay = new StackPane(toast);
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setPadding(new Insets(20, 0, 0, 0));
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(true);

        // Add overlay to THIS (the StackPane root)
        this.getChildren().add(overlay);

        // Slide down + fade in
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), toast);
        slideIn.setFromY(-40);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition showAnim = new ParallelTransition(slideIn, fadeIn);

        // After 2s, fade out + slide up
        PauseTransition pause = new PauseTransition(Duration.millis(2000));

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toast);
        slideOut.setFromY(0);
        slideOut.setToY(-25);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        ParallelTransition hideAnim = new ParallelTransition(slideOut, fadeOut);
        hideAnim.setOnFinished(ev -> this.getChildren().remove(overlay));

        SequentialTransition seq = new SequentialTransition(showAnim, pause, hideAnim);
        seq.play();
    }
}
