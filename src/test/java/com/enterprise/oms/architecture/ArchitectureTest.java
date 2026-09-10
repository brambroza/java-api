package com.enterprise.oms.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executable architecture: these rules fail the build when someone bypasses the layering
 * (e.g. a controller calling a JPA repository, or the domain importing Spring MVC).
 */
@AnalyzeClasses(packages = "com.enterprise.oms", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Api").definedBy("..api..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api");

    @ArchTest
    static final ArchRule domain_is_framework_light = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat(resideInAnyPackage(
                    "org.springframework.web..", "org.springframework.stereotype..",
                    "org.springframework.transaction..", "org.springframework.security..",
                    "jakarta.servlet..", "tools.jackson.."));

    @ArchTest
    static final ArchRule controllers_live_in_api = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule jpa_repositories_live_in_infrastructure = classes()
            .that().areAssignableTo(JpaRepository.class)
            .should().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule transactional_classes_are_application_or_infrastructure = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule transactional_methods_are_application_or_infrastructure = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule modules_have_no_cycles = slices()
            .matching("com.enterprise.oms.(*)..")
            .should().beFreeOfCycles();
}
