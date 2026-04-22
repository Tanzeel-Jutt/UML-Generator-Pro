package com.umlgenerator.core.generator;

import com.umlgenerator.core.model.*;

/**
 * Abstract base class for diagram generators (Template Method pattern).
 * Provides common structure for generating different diagram types.
 * 
 * Platform-independent: no JavaFX dependencies.
 */
public abstract class DiagramGenerator<T> {

    /**
     * Template method: defines the overall generation process.
     */
    public final T generate(Object input) {
        validate(input);
        T result = doGenerate(input);
        postProcess(result);
        return result;
    }

    /**
     * Validate input before generation. Override with specific validation.
     */
    protected void validate(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
    }

    /**
     * Perform the actual generation. Must be implemented by subclasses.
     */
    protected abstract T doGenerate(Object input);

    /**
     * Post-processing step. Override for any cleanup/optimization.
     */
    protected void postProcess(T result) {
        // Default: no post-processing
    }

    /**
     * Get the name of this generator type.
     */
    public abstract String getGeneratorName();
}
