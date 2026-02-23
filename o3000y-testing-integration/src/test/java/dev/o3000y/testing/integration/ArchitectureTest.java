package dev.o3000y.testing.integration;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.o3000y");
  }

  @Test
  void model_dependsOnNothing() {
    ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("dev.o3000y.model..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("dev.o3000y.model..", "java..", "javax..");
    rule.check(classes);
  }

  @Test
  void storage_doesNotDependOnIngestion() {
    ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("dev.o3000y.storage..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("dev.o3000y.ingestion..");
    rule.check(classes);
  }

  @Test
  void query_doesNotDependOnIngestion() {
    ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("dev.o3000y.query..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("dev.o3000y.ingestion..");
    rule.check(classes);
  }

  @Test
  void ingestion_doesNotDependOnQuery() {
    ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("dev.o3000y.ingestion..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("dev.o3000y.query..");
    rule.check(classes);
  }
}
