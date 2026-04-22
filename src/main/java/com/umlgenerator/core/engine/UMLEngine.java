package com.umlgenerator.core.engine;

import com.umlgenerator.core.generator.*;
import com.umlgenerator.core.model.*;
import com.umlgenerator.core.parser.*;

/**
 * Facade/Orchestrator for the entire UML generation pipeline.
 * Coordinates parsing, generation, and provides a clean API for the UI layer.
 * 
 * Platform-independent: use this same engine on Android.
 * 
 * Design Patterns:
 * - Facade: simplifies complex subsystem
 * - Strategy: delegates to specific parsers
 * - Template Method: generators follow standard pipeline
 */
public class UMLEngine {

    private final UMLClassDiagramGenerator umlGenerator;
    private final ERDiagramGenerator erGenerator;
    private final EERDDiagramGenerator eerdGenerator;
    private final RelationalMappingGenerator relationalGenerator;

    public UMLEngine() {
        this.umlGenerator = new UMLClassDiagramGenerator();
        this.erGenerator = new ERDiagramGenerator();
        this.eerdGenerator = new EERDDiagramGenerator();
        this.relationalGenerator = new RelationalMappingGenerator();
    }

    /**
     * Generate UML Class Diagram from source code.
     * 
     * @param sourceCode The source code to parse
     * @param language   The programming language (e.g., "Java", "Python", "C++", "SQL")
     * @return UMLDiagram ready for rendering
     */
    public UMLDiagram generateUMLDiagram(String sourceCode, String language) {
        LanguageParser parser = getParser(language, sourceCode);
        UMLDiagram diagram = parser.parseToUML(sourceCode);
        return umlGenerator.generate(diagram);
    }

    /**
     * Generate UML Class Diagram from multiple source files.
     */
    public UMLDiagram generateProjectUML(java.util.List<String> sources, String language) {
        if (sources == null || sources.isEmpty()) return new UMLDiagram();
        
        UMLDiagram masterDiagram = new UMLDiagram();
        LanguageParser parser = getParser(language, sources.get(0));
        
        for (String code : sources) {
            UMLDiagram fileDiagram = parser.parseToUML(code);
            masterDiagram.merge(fileDiagram);
        }
        
        return umlGenerator.generate(masterDiagram);
    }

    /**
     * Generate ER Diagram from SQL code.
     * 
     * @param sqlCode SQL DDL source code
     * @return ERDiagram ready for rendering
     */
    public ERDiagram generateERDiagram(String sqlCode) {
        SQLCodeParser sqlParser = new SQLCodeParser();
        ERDiagram diagram = sqlParser.parseToER(sqlCode);
        if (diagram != null) {
            return erGenerator.generate(diagram);
        }
        return new ERDiagram("Empty ER Diagram");
    }

    /**
     * Generate Enhanced ER (EERD) Diagram from SQL code.
     * 
     * @param sqlCode SQL DDL source code
     * @return ERDiagram with EERD features
     */
    public ERDiagram generateEERDDiagram(String sqlCode) {
        SQLCodeParser sqlParser = new SQLCodeParser();
        ERDiagram diagram = sqlParser.parseToER(sqlCode);
        if (diagram != null) {
            return eerdGenerator.generate(diagram);
        }
        return new ERDiagram("Empty EERD");
    }

    /**
     * Generate Relational Schema Mapping from SQL code.
     * 
     * @param sqlCode SQL DDL source code
     * @return RelationalSchema with mapped tables
     */
    public RelationalSchema generateRelationalMapping(String sqlCode) {
        ERDiagram erDiagram = generateERDiagram(sqlCode);
        return relationalGenerator.generate(erDiagram);
    }

    /**
     * Generate Relational Mapping from an existing ER diagram.
     * 
     * @param erDiagram Existing ER diagram
     * @return RelationalSchema
     */
    public RelationalSchema generateRelationalMapping(ERDiagram erDiagram) {
        return relationalGenerator.generate(erDiagram);
    }

    /**
     * Auto-detect language and generate UML.
     * 
     * @param sourceCode Source code to analyze
     * @return UMLDiagram
     */
    public UMLDiagram autoGenerateUML(String sourceCode) {
        LanguageParser parser = ParserFactory.detectParser(sourceCode);
        UMLDiagram diagram = parser.parseToUML(sourceCode);
        return umlGenerator.generate(diagram);
    }

    /**
     * Get the appropriate parser for a language.
     */
    private LanguageParser getParser(String language, String sourceCode) {
        LanguageParser parser = ParserFactory.getParser(language);
        if (parser == null) {
            parser = ParserFactory.detectParser(sourceCode);
        }
        return parser;
    }

    /**
     * Get available languages.
     */
    public String[] getAvailableLanguages() {
        return ParserFactory.getAvailableLanguages();
    }

    /**
     * Check if a language supports ER diagram generation.
     */
    public boolean supportsER(String language) {
        LanguageParser parser = ParserFactory.getParser(language);
        return parser != null && parser.supportsERDiagram();
    }
}
