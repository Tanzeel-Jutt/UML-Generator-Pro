module com.umlgenerator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires com.google.gson;
    requires java.desktop;
    requires com.github.javaparser.core;

    opens com.umlgenerator.ui to javafx.fxml;
    opens com.umlgenerator.core.model to com.google.gson;

    exports com.umlgenerator.ui;
    exports com.umlgenerator.core.model;
    exports com.umlgenerator.core.parser;
    exports com.umlgenerator.core.generator;
    exports com.umlgenerator.core.engine;
    exports com.umlgenerator.ui.components;
    exports com.umlgenerator.util;
}
