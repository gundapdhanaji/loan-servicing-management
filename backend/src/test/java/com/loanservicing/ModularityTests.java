package com.loanservicing;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * THE test that keeps the project ready for microservices.
 *
 * It fails the build if:
 *   - a module uses another module's internal classes (e.g. payment -> loan.internal.Loan)
 *   - two modules depend on each other in a cycle (loan -> payment -> loan)
 *
 * It also writes module diagrams to target/spring-modulith-docs (open the .puml files
 * with any PlantUML viewer) - a map of your future microservices.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(LoanServicingApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.forEach(System.out::println);
        modules.verify();
    }

    @Test
    void writeModuleDiagrams() {
        new Documenter(modules).writeDocumentation();
    }
}
