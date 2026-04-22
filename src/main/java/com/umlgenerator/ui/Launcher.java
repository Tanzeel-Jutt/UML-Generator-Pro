package com.umlgenerator.ui;

/**
 * Launcher class to avoid JavaFX module issues.
 * This class does NOT extend Application, allowing it to be
 * the main entry point without requiring JavaFX modules on the module path.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}
