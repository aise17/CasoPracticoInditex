---
name: Implementación Product Ranking
overview: Implementar el servicio de ranking de productos (Java 17, Spring Boot 3.x, MongoDB, arquitectura hexagonal + DDD táctico) siguiendo fielmente los specs de docs/specs, en el orden domain-first definido en el spec 007.
todos:
  - id: env-setup
    content: "Fase 0: instalar JDK 17, verificar Docker, generar esqueleto Spring Boot 3 con mvnw y pom completo"
    status: pending
  - id: domain
    content: "Fase 1: implementar dominio completo (VOs con equals/hashCode, Product, criterios Strategy, ProductRankingService con tie-breaker)"
    status: pending
  - id: domain-tests
    content: "Fase 2: test mothers + tests unitarios de dominio, checkpoint mvnw test en verde"
    status: pending
  - id: application
    content: "Fase 3: puertos in/out, RankProductsCommand, RankProductsService + test con InMemoryProductRepository"
    status: pending
  - id: mongo
    content: "Fase 4: adapter MongoDB (document, mapper, repositorio), configuracion Spring, seed, docker-compose, application.yml + tests integracion Testcontainers"
    status: pending
  - id: rest
    content: "Fase 5: adapter REST (DTOs, mapper, controller, exception handler) + tests @WebMvcTest"
    status: pending
  - id: e2e-arch
    content: "Fase 6: E2E con Rest Assured (8 casos) + HexagonalArchitectureTest con ArchUnit"
    status: pending
  - id: docs-cleanup
    content: "Fase 7: README con decisiones de diseno, limpieza final, suite completa en verde y verificacion manual"
    status: pending
isProject: false
---

# Plan de Implementación — Product Ranking Backend Tools

Basado en [docs/specs/007-implementation-plan.md](docs/specs/007-implementation-plan.md) y el resto de specs (001–006). Orden domain-first: dominio → aplicación → infraestructura, con tests en cada fase.

## Fase 0 — Entorno (el workspace está vacío salvo docs/)

- Instalar JDK 17 vía Homebrew (`brew install --cask temurin@17`) — no hay ningún JDK instalado.
- Verificar que el daemon de Docker está corriendo (necesario para Testcontainers y docker-compose).
- Generar el esqueleto del proyecto con Spring Initializr (curl a start.spring.io): Maven + Java 17 + Spring Boot 3.x, dependencias `web` y `data-mongodb`. Esto trae `mvnw` (no hay Maven instalado), `.gitignore` y `ProductRankingApplication` (clase main).
- `git init` del repositorio (sin commits salvo que se pidan).
- Paquete base: `com.backendtools.productranking`. Añadir al `pom.xml`: Rest Assured, Testcontainers (BOM + mongodb + junit-jupiter), ArchUnit. **Sin** starter de Bean Validation (decisión 004 §9.1).
- Toda la suite corre con `./mvnw test` (006 §20): sin split surefire/failsafe.

## Fase 1 — Dominio (spec 001 y 003)

Crear en `domain/model`: `ProductId`, `ProductName`, `SalesUnits`, `Size`, `SizeStock`, `Stock`, `Score`, `CriterionWeight`, `RankingWeights`, `Product`, `RankedProduct`, `ProductRanking`.
Crear en `domain/ranking`: `CriterionName`, `RankingCriterion`, `SalesUnitsCriterion`, `StockRatioCriterion`, `ProductRankingService`.

Reglas clave de los specs:
- Todos los VOs: inmutables + `equals`/`hashCode` por valor + `toString` (001 §5.0).
- `Stock`: rechaza vacío y **tallas duplicadas** (001 §5.6).
- `RankedProduct.highestScoreFirst()`: score descendente + tie-breaker por id ascendente (001 §8.1).
- `RankingWeights.weightFor()` devuelve peso 0 si falta el criterio.
- Sin anotaciones Spring/Mongo/Jackson en el dominio.

## Fase 2 — Tests de dominio (spec 006 §8–10)

- Test mothers de dominio: `ProductTestMother`, `RankingWeightsTestMother`, `ProductRankingServiceTestMother` (el cuarto, `ProductDocumentTestMother`, es de infraestructura y se crea en Fase 4).
- Tests unitarios de todos los VOs (incluida igualdad por valor y tallas duplicadas) y criterios.
- `ProductRankingServiceTest` con los casos del 003 §20: orden principal `5,1,3,2,6,4` (pesos 0.7/0.3), stock-only `2,3,4,6,1,5`, peso ausente = 0, tie-breaker con **input en orden inverso** (006 §10.3), y el edge case **todos los pesos a 0** → orden por tie-breaker (003 §15.1, sin test asignado en los specs: añadirlo aquí y reflejarlo en 006 §10).
- Checkpoint: `./mvnw test` en verde sin Spring ni Mongo.

## Fase 3 — Capa de aplicación (spec 002 §5–8)

- `application/port/in/RankProductsUseCase`, `application/port/out/ProductRepository`.
- `application/usecase/RankProductsCommand` y `RankProductsService`.
- Test `RankProductsServiceTest` con `InMemoryProductRepository` (sin Spring).

## Fase 4 — Infraestructura MongoDB (spec 005)

- `infrastructure/persistence/mongo`: `ProductDocument`, `SpringDataMongoProductRepository`, `ProductMongoMapper`, `MongoProductRepository`.
- `infrastructure/configuration`: `ProductRankingConfiguration` (beans de dominio/aplicación) y `MongoSeedConfiguration` (seed solo si la colección está vacía).
- `application.yml` (uri mongo + puerto 8080) y `docker-compose.yml` (mongo:7, **sin** atributo `version`).
- `ProductDocumentTestMother` para los tests de infraestructura (006 §15).
- `MongoProductRepositoryIntegrationTest` con `@DataMongoTest` + Testcontainers (3 casos del 005 §22). Ojo: `@DataMongoTest` no escanea `@Component`/`@Repository`, así que el test construye `ProductMongoMapper` y `MongoProductRepository` manualmente en `setUp` (005 §23).

## Fase 5 — Infraestructura REST (spec 004)

- `infrastructure/rest`: `ProductRankingRequest`, `RankingWeightsRequest`, `ProductRankingResponse`, `RankedProductResponse`, `ErrorResponse`, `ProductRankingRestMapper`, `ProductRankingController` (`POST /api/products/ranking`), `RestExceptionHandler`.
- Decisiones clave: sin `@Valid`/`@NotNull` (el mapper valida weights null → "Ranking weights cannot be empty"); handler solo para `IllegalArgumentException` y `HttpMessageNotReadableException`; redondeo a 2 decimales solo en respuesta; criterios desconocidos ignorados (004 §5.6).
- Tests de controller con `@WebMvcTest` + `@MockitoBean` (3 casos del 006 §13).

## Fase 6 — Tests E2E y arquitectura (spec 006 §14 y §18)

- `ProductRankingE2ETest`: `@SpringBootTest(RANDOM_PORT)` + Rest Assured + Testcontainers. 8 casos: ranking 0.7/0.3 → `5,1,3,2,6,4`, stock-only, peso negativo 400, weights vacío 400, JSON malformado 400, BD vacía → lista vacía, criterio desconocido ignorado (con peso válido presente → 200), y **solo criterios desconocidos → 400** "Ranking weights cannot be empty" (edge case del 004 §5.6 sin test asignado en los specs: añadirlo y reflejarlo en 006 §14.1).
- Cada test E2E prepara sus propios datos con `deleteAll` + `saveAll`, independiente del seed de arranque (006 §14.2).
- `HexagonalArchitectureTest` con ArchUnit: dominio sin Spring/Mongo, aplicación sin infraestructura, controllers y documents en sus paquetes.

## Fase 7 — Documentación y cierre (spec 007 fases 9–10)

- `README.md`: descripción, arquitectura, how-to-run/test, ejemplos curl, decisiones de diseño (incluyendo Strategy vs decorador, double vs BigDecimal, por qué no Bean Validation, tie-breaker).
- Opcional (004 §15): OpenAPI con springdoc para la presentación. Decidir al llegar; si no se incluye, no añadir la dependencia.
- Limpieza final: sin código sin usar, sin comentarios narrativos, todo en inglés, formato consistente.
- Verificación completa: `./mvnw test` con toda la suite en verde y arranque manual con docker-compose + curl del ejemplo principal.

## Definition of Done

La del spec 007 §54: app arranca, seed carga los 6 productos, el endpoint devuelve `5,1,3,2,6,4` con pesos 0.7/0.3, y pasan unitarios + integración + E2E + ArchUnit.