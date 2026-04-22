package com.umlgenerator.core.parser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory for creating language-specific parsers.
 * Uses Factory design pattern - easily extensible for new languages.
 * 
 * Platform-independent: can be reused in Android module.
 */
public class ParserFactory {

    private static final Map<String, LanguageParser> PARSERS = new LinkedHashMap<>();

    static {
        registerParser(new JavaCodeParser());
        registerParser(new PythonCodeParser());
        registerParser(new CppCodeParser());
        registerParser(new SQLCodeParser());
    }

    /**
     * Register a new parser. Allows extending with custom parsers.
     */
    public static void registerParser(LanguageParser parser) {
        PARSERS.put(parser.getLanguageName().toLowerCase(), parser);
    }

    /**
     * Get parser by language name.
     * 
     * @param language Language name (e.g., "Java", "Python", "C++", "SQL")
     * @return The parser, or null if not found
     */
    public static LanguageParser getParser(String language) {
        return PARSERS.get(language.toLowerCase());
    }

    /**
     * Auto-detect language from source code and return appropriate parser.
     * 
     * @param sourceCode The source code to analyze
     * @return Best matching parser, or Java parser as default
     */
    public static LanguageParser detectParser(String sourceCode) {
        for (LanguageParser parser : PARSERS.values()) {
            if (parser.canParse(sourceCode)) {
                return parser;
            }
        }
        // Default to Java
        return PARSERS.get("java");
    }

    /**
     * Get all available language names.
     */
    public static String[] getAvailableLanguages() {
        return PARSERS.values().stream()
                .map(LanguageParser::getLanguageName)
                .toArray(String[]::new);
    }

    /**
     * Get all registered parsers.
     */
    public static Map<String, LanguageParser> getAllParsers() {
        return new LinkedHashMap<>(PARSERS);
    }

    /**
     * Check if a language is supported.
     */
    public static boolean isSupported(String language) {
        return PARSERS.containsKey(language.toLowerCase());
    }
}
