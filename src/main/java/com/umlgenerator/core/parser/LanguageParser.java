package com.umlgenerator.core.parser;

import com.umlgenerator.core.model.UMLDiagram;
import com.umlgenerator.core.model.ERDiagram;

/**
 * Strategy interface for language-specific code parsers.
 * Each supported language implements this interface.
 * 
 * Platform-independent: no UI dependencies.
 * Android-portable: implement this interface in Android module.
 */
public interface LanguageParser {

    /**
     * Get the name of the language this parser handles.
     */
    String getLanguageName();

    /**
     * Get file extensions this parser supports.
     */
    String[] getSupportedExtensions();

    /**
     * Parse source code and generate a UML class diagram.
     * 
     * @param sourceCode The raw source code (may contain multiple classes/files)
     * @return UMLDiagram containing all parsed classes and relationships
     */
    UMLDiagram parseToUML(String sourceCode);

    /**
     * Check if this parser supports ER diagram generation.
     * Typically only SQL parser supports this.
     */
    default boolean supportsERDiagram() {
        return false;
    }

    /**
     * Parse source code and generate an ER diagram.
     * Only supported by SQL parser by default.
     * 
     * @param sourceCode The raw source code
     * @return ERDiagram or null if not supported
     */
    default ERDiagram parseToER(String sourceCode) {
        return null;
    }

    /**
     * Validate if the given source code can be parsed by this parser.
     * 
     * @param sourceCode The raw source code
     * @return true if the code appears to be valid for this language
     */
    boolean canParse(String sourceCode);
}
