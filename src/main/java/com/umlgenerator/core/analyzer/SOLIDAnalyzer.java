package com.umlgenerator.core.analyzer;

import com.umlgenerator.core.generator.BoilerplateCodeGenerator.ParsedClass;
import com.umlgenerator.core.generator.BoilerplateCodeGenerator.ParsedAttribute;
import com.umlgenerator.core.generator.BoilerplateCodeGenerator.ParsedMethod;
import com.umlgenerator.core.model.UMLClass;
import com.umlgenerator.core.model.UMLAttribute;
import com.umlgenerator.core.model.UMLMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes UML classes for SOLID principle violations.
 */
public class SOLIDAnalyzer {

    public static class SOLIDViolation {
        public String principle;
        public String definition;
        public String criteria;
        public String rule;
        public List<String> locations = new ArrayList<>();

        public SOLIDViolation(String principle, String definition, String criteria, String rule) {
            this.principle = principle;
            this.definition = definition;
            this.criteria = criteria;
            this.rule = rule;
        }
        
        public void addLocation(String location) {
            this.locations.add(location);
        }
    }

    // --- Analyze ParsedClass (from UML to Code panel) ---
    public static List<SOLIDViolation> analyzeParsedClass(ParsedClass pc) {
        List<SOLIDViolation> violations = new ArrayList<>();

        // 1. SRP: Single Responsibility Principle + Cohesion
        boolean hasDb = false, hasLogic = false, hasUI = false;
        List<String> srpLocations = new ArrayList<>();
        
        for (ParsedMethod m : pc.methods) {
            String ln = m.name.toLowerCase();
            String body = m.body != null ? m.body.toLowerCase() : "";
            if (ln.contains("save") || ln.contains("update") || ln.contains("delete") || ln.contains("db") || ln.contains("sql") ||
                body.contains("insert into") || body.contains("jdbc") || body.contains("statement.execute") || body.contains("repository.save")) {
                hasDb = true; srpLocations.add("Method (DB): " + m.name);
            }
            if (ln.contains("calculate") || ln.contains("process") || ln.contains("check") || ln.contains("compute") ||
                body.contains("+") || body.contains("-") || body.contains("*") || body.contains("/") || body.contains("math.")) {
                hasLogic = true; srpLocations.add("Method (Logic): " + m.name);
            }
            if (ln.contains("print") || ln.contains("show") || ln.contains("render") || ln.contains("ui") || ln.contains("button") ||
                body.contains("system.out") || body.contains("jbutton") || body.contains("alert") || body.contains("scanner")) {
                hasUI = true; srpLocations.add("Method (UI): " + m.name);
            }
        }
        if ((hasDb && hasLogic) || (hasDb && hasUI) || (hasLogic && hasUI)) {
            SOLIDViolation srp = new SOLIDViolation(
                "Single Responsibility Principle (SRP) / Low Cohesion",
                "A class must encapsulate only one system functionality or business logic.",
                "If a class is creating UI components, handling the database, and checking business rules (like calculations) simultaneously, this indicates an SRP violation and Low Cohesion.",
                "A class should have only one reason to change. For High Cohesion, methods should work together towards a single, unified purpose."
            );
            srp.locations.addAll(srpLocations);
            violations.add(srp);
        }

        // 2. OCP: Open/Closed Principle
        SOLIDViolation ocp = null;
        for (ParsedMethod m : pc.methods) {
            String ln = m.name.toLowerCase();
            if (ln.contains("type") || ln.contains("kind") || ln.contains("switch") || ln.contains("mode")) {
                if (ocp == null) {
                    ocp = new SOLIDViolation(
                        "Open/Closed Principle (OCP)",
                        "Software entities should be open for extension but closed for modification.",
                        "If you have to repeatedly edit if-else or switch statements in an existing class to handle specific types or modes, OCP is being violated.",
                        "New features should always be 'Added' via Polymorphism (Inheritance/Interfaces), not by 'Changing' existing procedural code."
                    );
                }
                ocp.addLocation("Method: " + m.name);
            }
        }
        if (ocp != null) violations.add(ocp);

        // 3. LSP: Liskov Substitution Principle
        SOLIDViolation lsp = null;
        for (ParsedMethod m : pc.methods) {
            String ln = m.name.toLowerCase();
            if (ln.contains("throw") || ln.contains("exception") || ln.contains("unsupported")) {
                if (lsp == null) {
                    lsp = new SOLIDViolation(
                        "Liskov Substitution Principle (LSP)",
                        "Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.",
                        "If you override a parent method in a subclass and leave it empty or throw an UnsupportedOperationException, LSP is being violated.",
                        "A subclass must always fulfill the structural and behavioral 'Contract' established by its Parent class."
                    );
                }
                lsp.addLocation("Method: " + m.name);
            }
        }
        if (lsp != null) violations.add(lsp);

        // 4. ISP: Interface Segregation Principle
        if ("interface".equalsIgnoreCase(pc.stereotype) && pc.methods.size() >= 5) {
            SOLIDViolation isp = new SOLIDViolation(
                "Interface Segregation Principle (ISP)",
                "No client should be forced to depend on methods it does not use.",
                "If there is a large interface with many methods, but an implementing class only needs a few and is forced to implement the rest as dummy methods, ISP is being violated.",
                "Large 'fat' interfaces should be split into smaller, more specific interfaces."
            );
            isp.addLocation("Total Methods: " + pc.methods.size() + " (Threshold is 4)");
            violations.add(isp);
        }

        // 5. DIP: Dependency Inversion Principle + Coupling
        SOLIDViolation dip = null;
        for (ParsedAttribute a : pc.attributes) {
            String type = a.type.toLowerCase();
            if (type.equals("arraylist") || type.equals("hashmap") || type.contains("mysql") || type.contains("impl")) {
                if (dip == null) dip = createDipViolation();
                dip.addLocation("Attribute: " + a.name + " (" + a.type + ")");
            }
        }
        for (ParsedMethod m : pc.methods) {
            for (String[] param : m.params) {
                String type = param[1].toLowerCase();
                if (type.equals("arraylist") || type.equals("hashmap") || type.contains("mysql") || type.contains("impl")) {
                    if (dip == null) dip = createDipViolation();
                    dip.addLocation("Method Parameter: " + m.name + "(" + param[1] + " " + param[0] + ")");
                }
            }
        }
        if (dip != null) violations.add(dip);

        return violations;
    }

    // --- Analyze UMLClass (from Code to UML panel) ---
    public static List<SOLIDViolation> analyzeUMLClass(UMLClass uc) {
        List<SOLIDViolation> violations = new ArrayList<>();

        // 1. SRP + Cohesion
        boolean hasDb = false, hasLogic = false, hasUI = false;
        List<String> srpLocations = new ArrayList<>();
        
        for (UMLMethod m : uc.getMethods()) {
            String ln = m.getName().toLowerCase();
            String body = m.getBody() != null ? m.getBody().toLowerCase() : "";
            if (ln.contains("save") || ln.contains("update") || ln.contains("delete") || ln.contains("db") || ln.contains("sql") ||
                body.contains("insert into") || body.contains("jdbc") || body.contains("statement.execute") || body.contains("repository.save")) {
                hasDb = true; srpLocations.add("Method (DB): " + m.getName());
            }
            if (ln.contains("calculate") || ln.contains("process") || ln.contains("compute") ||
                body.contains("math.") || (body.contains("return ") && (body.contains("*") || body.contains("/")))) {
                hasLogic = true; srpLocations.add("Method (Logic): " + m.getName());
            }
            if (ln.contains("print") || ln.contains("show") || ln.contains("render") || ln.contains("ui") || ln.contains("button") ||
                body.contains("system.out") || body.contains("jbutton") || body.contains("alert") || body.contains("scanner")) {
                hasUI = true; srpLocations.add("Method (UI): " + m.getName());
            }
        }
        if ((hasDb && hasLogic) || (hasDb && hasUI) || (hasLogic && hasUI)) {
            SOLIDViolation srp = new SOLIDViolation(
                "Single Responsibility Principle (SRP) / Low Cohesion",
                "A class must encapsulate only one system functionality or business logic.",
                "If a class is creating UI components, handling the database, and checking business rules (like calculations) simultaneously, this indicates an SRP violation and Low Cohesion.",
                "A class should have only one reason to change. For High Cohesion, methods should work together towards a single, unified purpose."
            );
            srp.locations.addAll(srpLocations);
            violations.add(srp);
        }

        // 2. OCP
        SOLIDViolation ocp = null;
        for (UMLMethod m : uc.getMethods()) {
            String ln = m.getName().toLowerCase();
            if (ln.contains("type") || ln.contains("kind") || ln.contains("switch") || ln.contains("mode")) {
                if (ocp == null) {
                    ocp = new SOLIDViolation(
                        "Open/Closed Principle (OCP)",
                        "Software entities should be open for extension but closed for modification.",
                        "If you have to repeatedly edit if-else or switch statements in an existing class to handle specific types or modes, OCP is being violated.",
                        "New features should always be 'Added' via Polymorphism (Inheritance/Interfaces), not by 'Changing' existing procedural code."
                    );
                }
                ocp.addLocation("Method: " + m.getName());
            }
        }
        if (ocp != null) violations.add(ocp);

        // 3. LSP
        SOLIDViolation lsp = null;
        if (!uc.getName().toLowerCase().contains("exception")) {
            for (UMLMethod m : uc.getMethods()) {
                if (m.isConstructor()) continue;
                String ln = m.getName().toLowerCase();
                if (ln.contains("throw") || ln.contains("exception") || ln.contains("unsupported")) {
                    if (lsp == null) {
                        lsp = new SOLIDViolation(
                            "Liskov Substitution Principle (LSP)",
                            "Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.",
                            "If you override a parent method in a subclass and leave it empty or throw an UnsupportedOperationException, LSP is being violated.",
                            "A subclass must always fulfill the structural and behavioral 'Contract' established by its Parent class."
                        );
                    }
                    lsp.addLocation("Method: " + m.getName());
                }
            }
        }
        if (lsp != null) violations.add(lsp);

        // 4. ISP
        if (uc.getClassType().name().contains("INTERFACE") && uc.getMethods().size() >= 5) {
            SOLIDViolation isp = new SOLIDViolation(
                "Interface Segregation Principle (ISP)",
                "No client should be forced to depend on methods it does not use.",
                "If there is a large interface with many methods, but an implementing class only needs a few and is forced to implement the rest as dummy methods, ISP is being violated.",
                "Large 'fat' interfaces should be split into smaller, more specific interfaces."
            );
            isp.addLocation("Total Methods: " + uc.getMethods().size() + " (Threshold is 4)");
            violations.add(isp);
        }

        // 5. DIP + Coupling
        SOLIDViolation dip = null;
        for (UMLAttribute a : uc.getAttributes()) {
            String type = a.getType().toLowerCase();
            if (type.equals("arraylist") || type.equals("hashmap") || type.contains("mysql") || type.contains("impl")) {
                if (dip == null) dip = createDipViolation();
                dip.addLocation("Attribute: " + a.getName() + " (" + a.getType() + ")");
            }
        }
        for (UMLMethod m : uc.getMethods()) {
            for (com.umlgenerator.core.model.Parameter p : m.getParameters()) {
                String type = p.getType().toLowerCase();
                if (type.equals("arraylist") || type.equals("hashmap") || type.contains("mysql") || type.contains("impl")) {
                    if (dip == null) dip = createDipViolation();
                    dip.addLocation("Method Parameter: " + m.getName() + "(" + p.getType() + " " + p.getName() + ")");
                }
            }
        }
        if (dip != null) violations.add(dip);

        return violations;
    }
    
    private static SOLIDViolation createDipViolation() {
        return new SOLIDViolation(
            "Dependency Inversion Principle (DIP) / Tight Coupling",
            "High-level modules should not depend on low-level modules. Both should depend on abstractions.",
            "If your class contains direct usages of concrete class implementations instead of their abstract interfaces, it results in 'Tightly Coupled' code and a DIP violation.",
            "Classes should always depend on Interfaces/Abstract classes rather than concrete implementations. Loose Coupling is preferred."
        );
    }
}
