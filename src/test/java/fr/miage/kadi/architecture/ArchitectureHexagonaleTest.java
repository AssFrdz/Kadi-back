package fr.miage.kadi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles de dépendance de l'architecture hexagonale, vérifiées à chaque build :
 * les dépendances pointent toujours vers le domaine, jamais l'inverse,
 * et un module ne touche jamais au domaine ni à l'application d'un autre.
 */
@AnalyzeClasses(packages = "fr.miage.kadi", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureHexagonaleTest {

    @ArchTest
    static final ArchRule leDomaineNeDependQueDeJava = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..config..", "org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule lApplicationIgnoreLesAdaptateursEtLeFramework = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..config..", "org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule lesAdaptateursEntrantsNePassentPasParLaPersistance = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule lesAdaptateursSortantsNeConnaissentPasLeWeb = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..");

    @ArchTest
    static final ArchRule lesDomainesDesModulesSontIndependants = slices()
            .matching("fr.miage.kadi.(*).domain..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule lesApplicationsDesModulesSontIndependantes = slices()
            .matching("fr.miage.kadi.(*).application..")
            .should().notDependOnEachOther();
}
