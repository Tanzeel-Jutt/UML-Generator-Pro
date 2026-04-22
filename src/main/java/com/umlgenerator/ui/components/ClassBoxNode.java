package com.umlgenerator.ui.components;

import com.umlgenerator.core.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * JavaFX component that renders a single UML class box.
 * Three-compartment layout: Name | Attributes | Methods
 * 
 * Supports:
 * - Stereotypes (<<abstract>>, <<interface>>, <<enum>>)
 * - Access modifier symbols (+, -, #, ~)
 * - Static underline, abstract italic
 * - Enum constants section
 * - Click-to-expand interaction
 */
public class ClassBoxNode extends VBox {

    private final UMLClass umlClass;
    private boolean isCompact;
    private Runnable onClickHandler;

    // Color scheme for different class types
    private static final String CLASS_HEADER_COLOR = "#1a1a2e";
    private static final String ABSTRACT_HEADER_COLOR = "#16213e";
    private static final String INTERFACE_HEADER_COLOR = "#0f3460";
    private static final String ENUM_HEADER_COLOR = "#533483";
    private static final String RECORD_HEADER_COLOR = "#2b2d42";

    public ClassBoxNode(UMLClass umlClass, boolean compact) {
        this.umlClass = umlClass;
        this.isCompact = compact;
        buildNode();
    }

    public ClassBoxNode(UMLClass umlClass) {
        this(umlClass, false);
    }

    public void setOnClickHandler(Runnable handler) {
        this.onClickHandler = handler;
    }

    private void buildNode() {
        setMinWidth(280);
        setMaxWidth(380);
        setPrefWidth(280);
        setPadding(Insets.EMPTY);
        setAlignment(Pos.TOP_CENTER);

        // Drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        setEffect(shadow);

        // Header section (class name + stereotype)
        VBox header = createHeader();
        getChildren().add(header);

        if (!isCompact) {
            // First separator line
            Separator sep1 = new Separator();
            sep1.setStyle("-fx-background-color: #334155; -fx-padding: 0;");
            getChildren().add(sep1);

            // Attributes section
            VBox attributesSection = createAttributesSection();
            getChildren().add(attributesSection);

            // Second separator line
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-background-color: #334155; -fx-padding: 0;");
            getChildren().add(sep2);

            // Methods section
            VBox methodsSection = createMethodsSection();
            getChildren().add(methodsSection);
        }

        // Click handler
        setOnMouseClicked(e -> {
            if (onClickHandler != null) onClickHandler.run();
        });

        // Hover effect
        setOnMouseEntered(e -> {
            setScaleX(1.02);
            setScaleY(1.02);
            shadow.setRadius(15);
            shadow.setColor(Color.rgb(99, 102, 241, 0.4));
        });
        setOnMouseExited(e -> {
            setScaleX(1.0);
            setScaleY(1.0);
            shadow.setRadius(10);
            shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        });
    }

    private VBox createHeader() {
        VBox header = new VBox(2);
        header.getStyleClass().add("class-box-header");
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setAlignment(Pos.CENTER);

        // Set background color based on class type
        String bgColor = switch (umlClass.getClassType()) {
            case ABSTRACT_CLASS -> ABSTRACT_HEADER_COLOR;
            case INTERFACE -> INTERFACE_HEADER_COLOR;
            case ENUM -> ENUM_HEADER_COLOR;
            case RECORD -> RECORD_HEADER_COLOR;
            default -> CLASS_HEADER_COLOR;
        };
        header.setStyle("-fx-background-color: " + bgColor + ";"
                + "-fx-background-radius: 8 8 0 0;");

        // Stereotype label
        if (umlClass.getClassType().hasStereotype()) {
            Label stereotypeLabel = new Label(umlClass.getClassType().getStereotype());
            stereotypeLabel.getStyleClass().add("stereotype-label");
            stereotypeLabel.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 10px;"
                    + "-fx-font-style: italic;");
            header.getChildren().add(stereotypeLabel);
        }

        // Class name
        Label nameLabel = new Label(umlClass.getName());
        nameLabel.getStyleClass().add("class-name-label");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        if (umlClass.getClassType() == ClassType.ABSTRACT_CLASS) {
            nameLabel.setStyle(nameLabel.getStyle() + "-fx-font-style: italic;");
        }
        header.getChildren().add(nameLabel);

        // Package name (if available)
        if (umlClass.getPackageName() != null && !umlClass.getPackageName().isEmpty()
                && !umlClass.getPackageName().equals("(external)")) {
            Label pkgLabel = new Label("(" + umlClass.getPackageName() + ")");
            pkgLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 9px;");
            header.getChildren().add(pkgLabel);
        }

        return header;
    }

    private VBox createAttributesSection() {
        VBox section = new VBox(1);
        section.getStyleClass().add("class-box-section");
        section.setPadding(new Insets(6, 10, 6, 10));
        section.setStyle("-fx-background-color: #1e1e2e;");

        // Enum constants first
        if (umlClass.getClassType() == ClassType.ENUM && !umlClass.getEnumConstants().isEmpty()) {
            Label enumTitle = new Label("«values»");
            enumTitle.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 9px; -fx-font-style: italic;");
            section.getChildren().add(enumTitle);

            for (String constant : umlClass.getEnumConstants()) {
                Label constLabel = new Label("  " + constant);
                constLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-family: 'Consolas';");
                section.getChildren().add(constLabel);
            }

            section.getChildren().add(new Separator());
        }

        if (umlClass.getAttributes().isEmpty()) {
            Label emptyLabel = new Label("  (no attributes)");
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-style: italic;");
            section.getChildren().add(emptyLabel);
        } else {
            for (UMLAttribute attr : umlClass.getAttributes()) {
                Label attrLabel = createAttributeLabel(attr);
                section.getChildren().add(attrLabel);
            }
        }

        return section;
    }

    private VBox createMethodsSection() {
        VBox section = new VBox(1);
        section.getStyleClass().add("class-box-section");
        section.setPadding(new Insets(6, 10, 8, 10));
        section.setStyle("-fx-background-color: #1e1e2e; -fx-background-radius: 0 0 8 8;");

        if (umlClass.getMethods().isEmpty()) {
            Label emptyLabel = new Label("  (no methods)");
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-style: italic;");
            section.getChildren().add(emptyLabel);
        } else {
            // Constructors first, then all other methods - flat list, standard UML
            for (UMLMethod method : umlClass.getConstructors()) {
                Label methodLabel = createMethodLabel(method);
                section.getChildren().add(methodLabel);
            }
            for (UMLMethod method : umlClass.getNonConstructorMethods()) {
                Label methodLabel = createMethodLabel(method);
                section.getChildren().add(methodLabel);
            }
        }

        return section;
    }

    private Label createAttributeLabel(UMLAttribute attr) {
        String text = attr.toUMLString().trim();
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        label.setWrapText(true);
        label.setMaxWidth(350);

        // Color based on access modifier
        String color = getAccessColor(attr.getAccessModifier());
        label.setStyle(label.getStyle() + "-fx-text-fill: " + color + ";");

        // Static = underline
        if (attr.isStatic()) {
            label.setUnderline(true);
        }

        return label;
    }

    private Label createMethodLabel(UMLMethod method) {
        String text = method.toUMLString().trim();
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        label.setWrapText(true);
        label.setMaxWidth(350);

        // Color based on access modifier
        String color = getAccessColor(method.getAccessModifier());
        label.setStyle(label.getStyle() + "-fx-text-fill: " + color + ";");

        // Static = underline
        if (method.isStatic()) {
            label.setUnderline(true);
        }

        // Abstract = italic
        if (method.isAbstract()) {
            label.setStyle(label.getStyle() + "-fx-font-style: italic;");
        }

        // Constructor in bold
        if (method.isConstructor()) {
            label.setStyle(label.getStyle() + "-fx-font-weight: bold;");
        }

        return label;
    }

    private String getAccessColor(AccessModifier access) {
        return switch (access) {
            case PUBLIC -> "#4ade80";
            case PRIVATE -> "#f87171";
            case PROTECTED -> "#fbbf24";
            case PACKAGE_PRIVATE -> "#60a5fa";
        };
    }

    public UMLClass getUmlClass() {
        return umlClass;
    }

    public double getCenterX() {
        return getLayoutX() + getBoundsInLocal().getWidth() / 2;
    }

    public double getCenterY() {
        return getLayoutY() + getBoundsInLocal().getHeight() / 2;
    }
}
