package com.autopecas.autopecas.arquitetura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * As regras da arquitetura hexagonal como teste executável.
 *
 * <p>Este é o mecanismo que substitui a separação em módulos Maven: qualquer import que fure a
 * fronteira do hexágono quebra a build, e não depende de disciplina de quem escreve o código.
 *
 * <p>A regra central é a direção da dependência: o domínio não conhece ninguém, a aplicação
 * conhece só o domínio, e os adapters conhecem os dois — nunca o contrário.
 */
@DisplayName("Arquitetura hexagonal")
class ArquiteturaHexagonalTest {

    private static final String PACOTE_RAIZ = "com.autopecas.autopecas";

    private static final String DOMINIO = PACOTE_RAIZ + ".domain..";
    private static final String APLICACAO = PACOTE_RAIZ + ".application..";
    private static final String ADAPTERS = PACOTE_RAIZ + ".adapter..";
    private static final String CONFIG = PACOTE_RAIZ + ".config..";
    private static final String SECURITY = PACOTE_RAIZ + ".security..";

    private static JavaClasses classes;

    @BeforeAll
    static void importarClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(PACOTE_RAIZ);
    }

    @Nested
    @DisplayName("O domínio é cético de tecnologia")
    class DominioCetico {

        @Test
        @DisplayName("não depende de Spring, JPA, Hibernate, Lombok, MapStruct nem Jackson")
        void naoDependeDeFrameworks() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "javax.persistence..",
                            "org.hibernate..",
                            "lombok..",
                            "org.mapstruct..",
                            "com.fasterxml..",
                            "tools.jackson..",
                            "io.swagger..")
                    .because("a regra de negócio precisa existir sem o mundo exterior");

            regra.check(classes);
        }

        @Test
        @DisplayName("não depende da aplicação, dos adapters nem da configuração")
        void naoDependeDasCamadasExternas() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(APLICACAO, ADAPTERS, CONFIG,
                            SECURITY)
                    .because("a dependência do hexágono aponta sempre para dentro");

            regra.check(classes);
        }

        @Test
        @DisplayName("não usa anotações de bean nem de persistência")
        void naoUsaAnotacoesDeInfraestrutura() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(DOMINIO)
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .orShould().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .orShould().beAnnotatedWith("jakarta.persistence.Entity")
                    .orShould().beAnnotatedWith("jakarta.persistence.Embeddable")
                    .because("quem transforma domínio em bean é a configuração, na borda");

            regra.check(classes);
        }
    }

    @Nested
    @DisplayName("A aplicação depende apenas do domínio")
    class AplicacaoIsolada {

        @Test
        @DisplayName("não depende de Spring, JPA nem Hibernate")
        void naoDependeDeFrameworks() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(APLICACAO)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "javax.persistence..",
                            "org.hibernate..",
                            "lombok..",
                            "org.mapstruct..")
                    .because("os casos de uso são orquestração pura, sem framework — nem Pageable "
                            + "do Spring Data atravessa as ports");

            regra.check(classes);
        }

        @Test
        @DisplayName("não depende dos adapters nem da configuração")
        void naoDependeDosAdapters() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(APLICACAO)
                    .should().dependOnClassesThat().resideInAnyPackage(ADAPTERS, CONFIG, SECURITY)
                    .because("a aplicação declara ports; quem as implementa é o adapter");

            regra.check(classes);
        }

        @Test
        @DisplayName("os casos de uso implementam alguma inbound port")
        void casosDeUsoImplementamPorts() {
            ArchRule regra = classes()
                    .that().resideInAPackage(PACOTE_RAIZ + ".application.usecase..")
                    .should().implement(
                            com.tngtech.archunit.base.DescribedPredicate.describe(
                                    "uma inbound port de application.port.in",
                                    javaClass -> javaClass.getPackageName()
                                            .startsWith(PACOTE_RAIZ + ".application.port.in")))
                    .because("todo caso de uso é acessado por uma port, nunca pela classe concreta");

            regra.check(classes);
        }
    }

    @Nested
    @DisplayName("Os adapters não se enxergam")
    class AdaptersIsolados {

        @Test
        @DisplayName("o adapter de entrada não depende do adapter de saída")
        void entradaNaoDependeDeSaida() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(PACOTE_RAIZ + ".adapter.in..")
                    .should().dependOnClassesThat().resideInAPackage(PACOTE_RAIZ + ".adapter.out..")
                    .because("HTTP fala com a aplicação, não com a persistência");

            regra.check(classes);
        }

        @Test
        @DisplayName("o adapter de saída não depende do adapter de entrada")
        void saidaNaoDependeDeEntrada() {
            ArchRule regra = noClasses()
                    .that().resideInAPackage(PACOTE_RAIZ + ".adapter.out.persistence.entity..")
                    .should().dependOnClassesThat().resideInAPackage(PACOTE_RAIZ + ".adapter.in..")
                    .because("as entidades JPA não conhecem a camada HTTP");

            regra.check(classes);
        }

        @Test
        @DisplayName("nenhuma entidade JPA escapa do adapter de persistência")
        void entidadesJpaFicamNoAdapter() {
            ArchRule regra = classes()
                    .that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage(PACOTE_RAIZ + ".adapter.out.persistence.entity..")
                    .because("entidade JPA é detalhe de persistência, não modelo de negócio");

            regra.check(classes);
        }
    }

    @Test
    @DisplayName("as camadas respeitam Domínio <- Aplicação <- Adapters")
    void camadasEmCebola() {
        ArchRule regra = layeredArchitecture().consideringAllDependencies()
                .layer("Dominio").definedBy(DOMINIO)
                .layer("Aplicacao").definedBy(APLICACAO)
                .layer("Adapters").definedBy(ADAPTERS)
                .layer("Configuracao").definedBy(CONFIG, SECURITY, PACOTE_RAIZ)

                .whereLayer("Configuracao").mayNotBeAccessedByAnyLayer()
                .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Configuracao")
                .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Adapters", "Configuracao")
                .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacao", "Adapters",
                        "Configuracao");

        regra.check(classes);
    }
}
