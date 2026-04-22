package com.umlgenerator.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for exporting diagrams to various formats.
 * Supports: PNG (high-res), SVG (planned), SQL script export.
 */
public class ExportUtil {

    /**
     * Export a JavaFX node as a high-resolution PNG image.
     * 
     * @param node  The JavaFX node to export
     * @param stage The parent stage for the file dialog
     * @param defaultName Default filename
     */
    public static void exportAsPNG(Node node, Stage stage, String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export as PNG");
        fileChooser.setInitialFileName(defaultName + ".png");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // High resolution export (2x scale)
                double scale = 2.0;
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.WHITE);
                params.setTransform(new Scale(scale, scale));

                WritableImage image = node.snapshot(params, null);
                BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bImage, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Export a JavaFX node as PNG to a specific file (no dialog).
     */
    public static void exportAsPNG(Node node, File file) throws IOException {
        double scale = 2.0;
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(new Scale(scale, scale));

        WritableImage image = node.snapshot(params, null);
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bImage, "png", file);
    }

    /**
     * Export SQL script to a file.
     * 
     * @param sql   The SQL content
     * @param stage The parent stage
     */
    public static void exportAsSQL(String sql, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export SQL Script");
        fileChooser.setInitialFileName("schema.sql");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQL Script", "*.sql"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(sql);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Export text content to a file.
     */
    public static void exportAsText(String content, Stage stage, String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export");
        fileChooser.setInitialFileName(defaultName + ".txt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
