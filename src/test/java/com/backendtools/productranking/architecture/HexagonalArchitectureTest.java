package com.backendtools.productranking.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("com.backendtools.productranking");

    @Test
    void domainShouldNotDependOnSpring() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainShouldNotDependOnMongo() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.mongodb..", "org.bson..")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainShouldNotDependOnApplicationOrInfrastructure() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void controllersShouldBeLocatedInInfrastructureRest() {
        classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..infrastructure.rest..")
            .check(PRODUCTION_CLASSES);
    }

    @Test
    void mongoDocumentsShouldBeLocatedInInfrastructurePersistenceMongo() {
        classes()
            .that().areAnnotatedWith(Document.class)
            .should().resideInAPackage("..infrastructure.persistence.mongo..")
            .check(PRODUCTION_CLASSES);
    }
}
