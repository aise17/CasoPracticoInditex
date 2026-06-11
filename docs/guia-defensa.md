# Guía de estudio y defensa — Product Ranking Service

Documento interno de preparación para la presentación de la prueba técnica. No forma parte del entregable de código: es material de estudio.

---

## 1. Resumen ejecutivo (elevator pitch, 60 segundos)

> "He implementado un servicio REST que ordena productos según una suma ponderada de criterios de negocio. El núcleo es un modelo de dominio rico siguiendo DDD táctico, aislado en una arquitectura hexagonal: el dominio no conoce Spring ni MongoDB. Los criterios de ordenación se modelan con el patrón Strategy, de modo que añadir un criterio nuevo es añadir una clase, no modificar el algoritmo. La pieza está cubierta con 71 tests en cinco niveles: unitarios, de aplicación, de integración con Testcontainers, E2E con Rest Assured y tests de arquitectura con ArchUnit que hacen cumplir las reglas de dependencia entre capas."

Stack: **Java 17, Spring Boot 3.5, Maven, MongoDB, Testcontainers, Rest Assured, ArchUnit**.

---

## 2. El problema y el contrato

Dado un listado de camisetas, ordenarlas por una puntuación que es la **suma ponderada** de criterios:

- **salesUnits**: puntuación = unidades vendidas.
- **stockRatio**: puntuación = proporción de tallas con stock disponible (escalada a 0–100).

Los **pesos los decide el consumidor de la API en cada petición**. Pueden añadirse criterios nuevos en el futuro.

### Contrato REST

`POST /api/products/ranking`

```json
{ "weights": { "salesUnits": 0.7, "stockRatio": 0.3 } }
```

Respuesta 200: lista de productos ordenados de mayor a menor score, cada uno con `id`, `name`, `salesUnits`, `stock` y `score` (redondeado a 2 decimales).

Errores (siempre 400 con `{"message": "..."}`):

| Caso | Mensaje |
| --- | --- |
| `weights` ausente o vacío | `Ranking weights cannot be empty` |
| Peso negativo | `Criterion weight cannot be negative` |
| JSON malformado o peso no numérico | `Malformed JSON request` |
| Solo criterios desconocidos | `Ranking weights cannot be empty` |

Reglas de tolerancia: los criterios desconocidos **se ignoran**; los pesos ausentes de criterios conocidos valen **cero**.

---

## 3. El cálculo del ranking, en detalle

### 3.1 Fórmula

```
score(p) = Σ  score_criterio(p) × peso_criterio
          criterios
```

Para los dos criterios actuales:

```
score(p) = salesUnits(p) × w_sales  +  (tallasConStock(p) / totalTallas(p)) × 100 × w_stock
```

### 3.2 Dónde vive cada pieza

| Pieza | Clase | Responsabilidad |
| --- | --- | --- |
| Puntuación por ventas | `SalesUnitsCriterion` | `score = product.salesUnits().value()` |
| Puntuación por stock | `StockRatioCriterion` | `score = stock.availabilityRatio() × 100` |
| Ratio de disponibilidad | `Stock.availabilityRatio()` | `tallas con unidades > 0 / total de tallas` |
| Suma ponderada y orden | `ProductRankingService.rank()` | orquesta criterios, pesos y ordenación |
| Comparador | `RankedProduct.highestScoreFirst()` | score desc + desempate por id asc |

### 3.3 Recorrido del algoritmo (`ProductRankingService.rank`)

1. Para cada producto, se recorre la lista de criterios (`RankingCriterion`).
2. Cada criterio calcula su `Score` para ese producto.
3. Se multiplica por el `CriterionWeight` que devuelve `RankingWeights.weightFor(name)` — si el peso no fue enviado, devuelve **peso cero** (el criterio no puntúa, pero no falla).
4. Los scores parciales se suman con `Score.add` (reduce con identidad `Score(0)`).
5. Se construye un `RankedProduct(product, finalScore)` por producto.
6. Se ordenan con `RankedProduct.highestScoreFirst()`: score descendente y, a igualdad de score, **id de producto ascendente (lexicográfico)**.
7. El resultado se encapsula en `ProductRanking` (lista inmutable).

### 3.4 Ejemplo numérico completo (pesos 0.7 / 0.3 — sabérselo de memoria)

Ratio de stock = tallas con unidades > 0 entre 3 tallas, escalado ×100:

| Id | Producto | Ventas | Stock S/M/L | Ratio ×100 | 0.7×ventas | 0.3×ratio | **Score** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 5 | CONTRASTING LACE | 650 | 0/1/0 | 33.33 | 455.0 | 10.0 | **465.0** |
| 1 | V-NECH BASIC | 100 | 4/9/0 | 66.67 | 70.0 | 20.0 | **90.0** |
| 3 | RAISED PRINT | 80 | 20/2/20 | 100 | 56.0 | 30.0 | **86.0** |
| 2 | CONTRASTING FABRIC | 50 | 35/9/9 | 100 | 35.0 | 30.0 | **65.0** |
| 6 | SLOGAN | 20 | 9/2/5 | 100 | 14.0 | 30.0 | **44.0** |
| 4 | PLEATED | 3 | 25/30/10 | 100 | 2.1 | 30.0 | **32.1** |

Orden resultante: **5, 1, 3, 2, 6, 4**.

### 3.5 Ejemplo del desempate (pesos 0 / 1, solo stock)

Los productos 2, 3, 4 y 6 tienen ratio 100 → empate a score 100. El desempate por id ascendente produce: **2, 3, 4, 6**, seguidos de 1 (66.67) y 5 (33.33).

> Detalle fino: como el sort de Java es estable, un test de desempate con la entrada ya ordenada por id pasaría aunque el tie-breaker no existiera. Por eso `shouldApplyDeterministicTieBreakerUsingProductId` mete los productos empatados **en orden inverso** (4 antes que 2) y verifica que salen `2, 4`.

### 3.6 Decisiones del algoritmo

- **Escalado ×100 del ratio de stock**: el ratio crudo (0–1) sería despreciable frente a ventas de cientos de unidades; con 0–100 ambos criterios juegan en órdenes de magnitud comparables. Es una decisión documentada y fácil de cambiar (constante `MAX_SCORE`).
- **No se normalizan las ventas**: el enunciado dice que la puntuación de ventas se basa en las unidades vendidas; cualquier normalización (min-max, z-score) sería inventar requisitos. Lo menciono como evolución posible.
- **Pesos sin restricción de suma**: no se exige que sumen 1. Son importancias relativas; exigir suma 1 restringiría al cliente sin necesidad.
- **Desempate determinista**: sin él, el orden de empatados dependería del orden de lectura de la BD → respuestas no reproducibles, tests frágiles.

---

## 4. Arquitectura hexagonal

```
            ┌──────────────────────────────────────────┐
  HTTP ───▶ │ infrastructure.rest (adapter primario)   │
            │  Controller → RestMapper → ErrorHandler  │
            └────────────────┬─────────────────────────┘
                             ▼ (puerto IN)
            ┌──────────────────────────────────────────┐
            │ application                              │
            │  RankProductsUseCase (port.in)           │
            │  RankProductsService + Command           │
            │  ProductRepository (port.out)            │
            └────────────────┬─────────────────────────┘
                             ▼
            ┌──────────────────────────────────────────┐
            │ domain (sin frameworks)                  │
            │  model: Product + value objects          │
            │  ranking: criterios + domain service     │
            └──────────────────────────────────────────┘
                             ▲ (implementa puerto OUT)
            ┌────────────────┴─────────────────────────┐
 MongoDB ◀─ │ infrastructure.persistence.mongo         │
            │  MongoProductRepository → Document/Mapper│
            └──────────────────────────────────────────┘
```

Puntos a verbalizar:

- **Regla de dependencia**: `infrastructure → application → domain`. El dominio no importa nada de Spring, Mongo ni de las otras capas. **Está garantizado por ArchUnit**, no es solo convención.
- **Inversión de dependencias**: `ProductRepository` (puerto de salida) se define en `application`; `MongoProductRepository` (infraestructura) lo implementa. La aplicación no sabe que existe Mongo.
- **El dominio y la aplicación se instancian a mano** en `ProductRankingConfiguration` (`@Bean`), sin anotaciones `@Service`/`@Component` en esas capas. Spring solo "toca" infraestructura.
- **Tres modelos distintos del producto**: `Product` (dominio), `ProductDocument` (persistencia), `RankedProductResponse` (REST). Cada uno evoluciona a su ritmo; los mappers (`ProductMongoMapper`, `ProductRankingRestMapper`) hacen la traducción en la frontera.

### Flujo completo de una petición

1. `ProductRankingController` recibe el JSON deserializado en `ProductRankingRequest` (DTO mudo de transporte).
2. `ProductRankingRestMapper.toCommand()` valida que hay pesos y construye `RankingWeights` → `RankProductsCommand`. Aquí los datos primitivos se convierten en value objects: **a partir de esta línea ya no circulan primitivos**.
3. `RankProductsService` (caso de uso) pide los productos al puerto `ProductRepository` y delega el cálculo en el domain service `ProductRankingService`.
4. `MongoProductRepository` lee `ProductDocument`s y los mapea a agregados `Product` (si un documento viola invariantes del dominio, explota: la corrupción de datos no entra al modelo).
5. El domain service devuelve `ProductRanking`; el mapper REST lo convierte a DTOs de respuesta redondeando el score a 2 decimales.
6. Cualquier `IllegalArgumentException` la captura `RestExceptionHandler` → 400 con mensaje.

---

## 5. DDD táctico aplicado

| Elemento | Clases | Por qué |
| --- | --- | --- |
| **Aggregate root** | `Product` | Tiene identidad (`ProductId`); igualdad por id. Es la unidad de consistencia. |
| **Value objects** | `ProductId`, `ProductName`, `SalesUnits`, `Size`, `SizeStock`, `Stock`, `Score`, `CriterionWeight`, `RankingWeights`, `RankedProduct`, `ProductRanking` | Sin identidad: dos `Score(90)` son el mismo valor. Inmutables, autovalidados, `equals`/`hashCode` por valor. |
| **Domain service** | `ProductRankingService` | El ranking compara *varios* productos: no pertenece a ningún agregado individual. |
| **Interfaz de dominio (Strategy)** | `RankingCriterion` + implementaciones | El concepto "criterio de ordenación" es del negocio, así que vive en el dominio. |

Cómo se **evita la anemia** (pregunta segura):

- Los VOs **validan en el constructor**: `SalesUnits` rechaza negativos, `Stock` rechaza vacío y tallas duplicadas, `Score` rechaza NaN/Infinity, `CriterionWeight` rechaza negativos. Es imposible construir un objeto de dominio inválido.
- El **comportamiento vive junto a los datos**: `Stock.availabilityRatio()`, `SizeStock.hasAvailableUnits()`, `Score.add()/multiplyBy()`, `RankingWeights.weightFor()` (que encapsula el "peso ausente = cero"). No hay servicios sacándole las tripas a los objetos.
- **Sin setters ni getters anémicos** en el dominio: accessors estilo `value()`, objetos inmutables.
- Los únicos DTOs "tontos" (`ProductRankingRequest`, `ProductDocument`...) están **confinados en infraestructura**, donde ser tonto es su trabajo: son estructuras de transporte/persistencia, no modelo.

Igualdad — matiz importante:

- VOs: igualdad **por valor** (todos los campos).
- `Product` (entidad/agregado): igualdad **por identidad** (`ProductId` solamente). Dos cargas del mismo producto con datos distintos siguen siendo "el mismo producto".

---

## 6. Patrones y decisiones de diseño (con su defensa)

### 6.1 Strategy para los criterios

`RankingCriterion { name(); calculateScore(Product); }` con `SalesUnitsCriterion` y `StockRatioCriterion`. El domain service recibe la lista de estrategias por constructor.

- **Por qué Strategy y no herencia**: los criterios no comparten estado ni algoritmo común que justifique una jerarquía; solo comparten un contrato. Herencia acoplaría implementaciones a una clase base frágil.
- **Por qué no Decorator**: el decorador sirve para **envolver y enriquecer** un comportamiento (criterio A modifica el resultado de B). Aquí los criterios son **independientes y se combinan por suma ponderada externa**; no hay composición encadenada que decorar. El enunciado menciona herencia/decoradores como opciones a valorar: las valoré y las descarté con argumento.
- Beneficio Open/Closed: añadir un criterio = 1 clase nueva + registrarla. El algoritmo de ranking no se toca.

### 6.2 `double` vs `BigDecimal` para el score

Elegí `double`. Defensa: el score es un **valor relativo de ordenación**, no dinero; no se acumulan miles de operaciones que amplifiquen el error; IEEE 754 con estos órdenes de magnitud es más que suficiente y el código queda más legible. Mitigaciones: `Score` rechaza NaN/Infinity, el comparador usa `Double.compare`, y el redondeo a 2 decimales se hace **solo en la frontera REST** (`BigDecimal.HALF_UP`), nunca dentro del dominio. Si mañana el score tuviera implicación financiera, el cambio queda encapsulado en `Score`.

### 6.3 Sin Bean Validation (`@NotNull`, `@Valid`)

La validación vive en **dos sitios con responsabilidades distintas**: el mapper REST (forma de la petición: "hay pesos") y los value objects (invariantes de negocio: "el peso no es negativo"). Con Bean Validation tendría las invariantes duplicadas en anotaciones + dominio, y los mensajes de error los generaría Spring con otro formato, rompiendo el contrato de la API. Una sola fuente de verdad.

### 6.4 DTO de pesos tipado vs `Map<String, Double>` genérico

`RankingWeightsRequest` tiene campos explícitos `salesUnits`/`stockRatio`. Trade-off consciente: contrato autodocumentado y tipado (mejor para OpenAPI, autocompletado, evolución controlada) a cambio de que añadir un criterio requiera tocar el DTO. Lo considero correcto: **añadir un criterio es una evolución deliberada de la API**, no algo que deba pasar "gratis" y sin revisión. La alternativa (mapa genérico) la sé explicar si la piden.

### 6.5 Desconocidos se ignoran, ausentes valen cero

- Campo desconocido en `weights` → Jackson lo ignora (comportamiento por defecto): tolerancia con clientes adelantados a la API (compatibilidad hacia adelante).
- Criterio conocido sin peso → `RankingWeights.weightFor` devuelve cero: el criterio simplemente no puntúa.
- Pero **todos desconocidos / vacío** → el mapa efectivo queda vacío → 400, porque un ranking "sin criterios" no tiene semántica.

### 6.6 Java 17 + Spring Boot 3

El enunciado pide "Java 8 o superior". Java 17 es LTS, baseline de Boot 3, y aporta text blocks (tests legibles), `Stream.toList`-style APIs, mejores GCs. Elegir Java 8 en 2026 sería una decisión peor difícil de justificar.

### 6.7 Seed de datos

`MongoSeedConfiguration` inserta el dataset del enunciado al arrancar **solo si la colección está vacía** (idempotente). Es infraestructura de demo, no lógica de negocio. Los tests **nunca** dependen del seed: cada test prepara sus propios datos.

---

## 7. Persistencia

- `ProductDocument`: documento Mongo con tipos primitivos y `Map<String, Integer>` para el stock (`{"S": 4, "M": 9, "L": 0}`). Anotado con `@Document(collection = "products")`. Constructor sin args `protected` exigido por el mapping de Spring Data.
- `SpringDataMongoProductRepository extends MongoRepository`: interfaz técnica de Spring Data.
- `MongoProductRepository`: **el adapter real** que implementa el puerto `ProductRepository` y traduce documento ⇄ dominio con `ProductMongoMapper`.
- Si un documento contiene datos inválidos (p. ej. ventas negativas), el mapper lanza `IllegalArgumentException` al construir los VOs: **el dominio actúa de barrera anticorrupción** frente a datos corruptos. Hay un test de integración que lo verifica.

---

## 8. Estrategia de testing (71 tests)

| Nivel | Qué prueba | Herramientas | Velocidad |
| --- | --- | --- | --- |
| Unitario dominio (49) | invariantes de VOs, criterios, algoritmo completo | JUnit 5 + AssertJ, sin Spring | ms |
| Unitario aplicación (2) | orquestación del caso de uso | fake `InMemoryProductRepository` (ni Mockito hace falta) | ms |
| Slice REST (3) | mapeo HTTP, códigos y mensajes de error | `@WebMvcTest` + MockMvc + `@MockitoBean` | ~1s |
| Integración persistencia (3) | mapping documento⇄dominio contra Mongo real | `@DataMongoTest` + Testcontainers | ~3s |
| E2E (8) | flujo completo HTTP→Mongo→HTTP | `@SpringBootTest(RANDOM_PORT)` + Rest Assured + Testcontainers | ~3s |
| Arquitectura (6) | reglas de dependencia entre capas | ArchUnit | <1s |

Puntos defendibles:

- **Integración ≠ E2E** (lo pide el enunciado explícitamente): el test de integración prueba **un adapter aislado** contra una dependencia real (repositorio + Mongo); el E2E levanta **toda la aplicación** y entra por HTTP con Rest Assured. Están en paquetes distintos (`infrastructure.persistence.mongo` vs `e2e`) y con nombres distintos (`*IntegrationTest` vs `*E2ETest`).
- **Nombres que describen comportamiento**: `shouldApplyDeterministicTieBreakerUsingProductId`, no `test1`.
- **Evitar Assertion Roulette** (test smell del enunciado): una aserción lógica por test; cuando hay varias físicas (p. ej. en el test de mapeo), van sobre el mismo objeto y con mensajes claros vía AssertJ encadenado.
- **Test mothers** (`ProductTestMother`, `RankingWeightsTestMother`, `ProductDocumentTestMother`): centralizan la construcción de datos de test, evitan duplicación y dan nombres semánticos (`salesOnly()`, `initialDataset()`).
- **Testcontainers**: Mongo real efímero por suite; ni mocks de base de datos ni dependencia de un Mongo local instalado.
- Los 8 casos E2E cubren los 5 criterios de aceptación originales + lista vacía + criterios desconocidos + solo-desconocidos.

### Las 6 reglas ArchUnit

1. El dominio no depende de Spring.
2. El dominio no depende de Mongo (driver/bson).
3. El dominio no depende de aplicación ni infraestructura.
4. La aplicación no depende de infraestructura.
5. Todo `@RestController` vive en `infrastructure.rest`.
6. Todo `@Document` vive en `infrastructure.persistence.mongo`.

---

## 9. Preguntas probables del jurado y respuestas

### Arquitectura

**P: ¿Por qué hexagonal y no una arquitectura en capas clásica?**
R: La hexagonal hace explícitos los **puertos**: el dominio y la aplicación definen contratos y la infraestructura los implementa, con la dependencia siempre apuntando hacia dentro. En capas clásicas es habitual que el "servicio" acabe acoplado al repositorio JPA/Mongo concreto. Aquí puedo sustituir Mongo por Postgres o por una API externa escribiendo otro adapter, sin tocar dominio ni aplicación. Y lo demuestro: el test de aplicación usa un repositorio in-memory contra el mismo puerto.

**P: ¿Qué es un puerto y qué es un adapter en tu código?**
R: Puerto de entrada: `RankProductsUseCase` (lo que la aplicación ofrece). Puerto de salida: `ProductRepository` (lo que la aplicación necesita). Adapter primario: `ProductRankingController` (traduce HTTP → caso de uso). Adapter secundario: `MongoProductRepository` (implementa el puerto contra Mongo).

**P: ¿Por qué `RankProductsCommand` en vez de pasar `RankingWeights` directamente?**
R: Es el objeto de entrada del caso de uso. Hoy lleva solo los pesos, pero da un punto de extensión estable (paginación, filtros, categoría) sin romper la firma del puerto, y mantiene la frontera de la aplicación explícita.

**P: ¿Dónde están los `@Service`?**
R: No hay. Dominio y aplicación son clases puras instanciadas en `ProductRankingConfiguration` con `@Bean`. Así ni siquiera hay anotaciones de Spring fuera de infraestructura, y ArchUnit lo verifica.

### Dominio / DDD

**P: ¿Por qué `Product` es agregado y `RankedProduct` value object?**
R: `Product` tiene identidad propia y continuidad: el producto 1 sigue siendo el producto 1 aunque cambien sus ventas. `RankedProduct` es el resultado de un cálculo: un par (producto, score) sin identidad; dos resultados con los mismos valores son intercambiables. Por eso `Product` iguala por id y `RankedProduct` por valor.

**P: ¿Por qué tantos value objects? ¿No es sobre-ingeniería envolver un `int`?**
R: Es la cura de la *primitive obsession*. `SalesUnits` garantiza no-negatividad **una vez**, en el constructor, y ya nadie tiene que re-validar; el tipo documenta la firma (`rank(List<Product>, RankingWeights)` se lee solo); y evita errores de transposición de argumentos que con dos `double` serían silenciosos. El coste es una clase pequeña; el beneficio, invariantes centralizadas.

**P: ¿Por qué `ProductRankingService` es un domain service y no un método de `Product`?**
R: Porque rankear involucra una **colección** de productos y una comparación entre ellos. Ningún producto individual puede "ordenarse a sí mismo". Regla DDD: lógica de dominio que no encaja naturalmente en un agregado → domain service, sin estado, hablando en términos del modelo.

**P: ¿Por qué `Stock` rechaza tallas duplicadas?**
R: Es una invariante: `[S:4, S:9]` haría que `availabilityRatio` contara S dos veces y el ratio saldría mal. El agregado debe ser imposible de construir en estado inválido. Alternativa valorada: usar `Map<Size, Units>` que lo impide estructuralmente; mantuve la lista para preservar orden y el VO `SizeStock`, validando en el constructor.

**P: ¿Dónde está la Ubiquitous Language?**
R: En los nombres: `RankingCriterion`, `CriterionWeight`, `SalesUnits`, `StockRatio`, `RankedProduct`... son los términos del enunciado. No hay traducción mental entre lo que dice negocio y lo que dice el código.

### Algoritmo

**P: ¿Qué pasa si dos productos empatan?**
R: Desempate determinista por id ascendente, incluido en el comparador `highestScoreFirst()`. Sin él, el orden dependería del orden de inserción en la BD y la API no sería reproducible. (Contar el detalle del sort estable y el test con entrada invertida, §3.5 — esto suele impresionar.)

**P: ¿Y si todos los pesos son cero?**
R: Petición válida: todos los scores son 0 y devuelve el catálogo ordenado por id. No hay razón de negocio para prohibirla; está cubierta por test.

**P: ¿Por qué el ratio de stock se multiplica por 100?**
R: Para que su escala (0–100) sea comparable con ventas típicas; si fuese 0–1, el criterio sería irrelevante salvo con pesos enormes. Es una constante del criterio, documentada y testeada. Reconozco abiertamente que la "escala natural" de cada criterio es una decisión de producto que en un caso real validaría con negocio.

**P: ¿Los pesos deben sumar 1?**
R: No lo exijo: son importancias relativas, no una distribución de probabilidad. `{2, 1}` y `{0.66, 0.33}` producen el mismo orden. Si negocio lo quisiera, sería una validación de una línea en `RankingWeights`.

### REST / API

**P: ¿Por qué POST y no GET si no muta estado?**
R: Semánticamente es un cálculo parametrizado con un cuerpo estructurado. Con GET los pesos irían en query params: sin tipado, con problemas de codificación y caché accidental. POST con body es la convención aceptada para "operaciones de consulta con entrada compleja" (estilo search endpoints). Si me piden purismo REST, lo discuto encantado: también sería defendible `GET /products?salesUnitsWeight=0.7` para dos criterios.

**P: ¿Por qué devuelves 400 y no 422?**
R: Decisión de contrato: 400 para toda petición que el servidor no puede procesar por culpa del cliente, con mensaje específico en el body. 422 sería igual de válido; lo importante es la consistencia documentada.

**P: ¿Cómo evolucionarías la API sin romper clientes?**
R: Campos nuevos en `weights` son ya compatibles (los desconocidos se ignoran). Para cambios incompatibles: versionado de ruta (`/api/v2/...`). Y contrato OpenAPI publicado (springdoc) como siguiente paso natural.

### Persistencia

**P: ¿Por qué dos "repositorios" de Mongo?**
R: `SpringDataMongoProductRepository` es el detalle técnico (interfaz de Spring Data); `MongoProductRepository` es el adapter que implementa **mi** puerto y mapea a dominio. Si expusiera la interfaz de Spring Data como puerto, la aplicación dependería de tipos de Spring y la firma devolvería documentos, no agregados.

**P: ¿Qué pasa si en Mongo hay un producto corrupto?**
R: El mapper construye los VOs y la invariante explota con `IllegalArgumentException`: prefiero fallar ruidosamente a rankear con datos inválidos. Hay un test de integración que lo demuestra. En producción, según el caso, podría decidirse loggear y saltar el documento — sería un cambio localizado en el adapter.

**P: ¿`findAll()` escala con millones de productos?**
R: No, y es una limitación consciente acotada al caso (un listado de categoría). La evolución: paginar en el puerto, o precalcular scores… (ver §10, "qué me pueden pedir").

### Testing

**P: ¿Por qué no usas mocks en los tests de aplicación?**
R: Uso un **fake** (`InMemoryProductRepository`): más legible que un mock de Mockito, no acopla el test a "qué métodos se llamaron" sino al comportamiento observable. Mockito lo reservo para el slice web, donde sustituir el caso de uso es lo natural.

**P: ¿Por qué `@DataMongoTest` y no `@SpringBootTest` en integración?**
R: Slice mínimo: levanta solo la infraestructura de Mongo, no el contexto entero. Tests más rápidos y con un propósito claro: probar el adapter, no la aplicación.

**P: ¿Qué pasa si alguien mete un import de Spring en el dominio?**
R: `HexagonalArchitectureTest` falla en CI. La arquitectura no es una convención de equipo, es una regla ejecutable.

### Calidad / proceso

**P: Usaste IA. ¿Qué decidiste tú?**
R: Trabajé con specs: antes de escribir código definí el modelo de dominio, la arquitectura, el algoritmo (incl. desempate y casos límite), el contrato REST con sus mensajes de error exactos y la estrategia de testing. La IA aceleró la mecanografía; las decisiones (Strategy vs decorator, double vs BigDecimal, no Bean Validation, igualdad por id en el agregado, escala ×100, tie-breaker...) están razonadas y las defiendo una a una. Además detecté y corregí incoherencias entre specs durante la revisión (p. ej. mensajes de error que Bean Validation no podía producir).

**P: ¿Qué problemas reales te encontraste?**
R: Dos buenos ejemplos de criterio técnico: (1) Docker Engine 29 exige API ≥ 1.44 y Testcontainers 1.x negocia 1.32 → diagnostiqué el 400 contra el socket con curl y lo fijé con `docker-java.properties`; (2) el engine JUnit 5 de ArchUnit reportaba 0 tests con Surefire — lo verifiqué con una regla canario que debía fallar, y lo reescribí como `@Test` planos con `ClassFileImporter`, más explícito y con una dependencia menos.

---

## 10. Qué me pueden pedir añadir en vivo (y cómo lo haría)

### 10.1 "Añade un criterio nuevo" (la más probable — p. ej. *novedad* o *margen*)

Pasos exactos (ensayar):

1. `CriterionName`: añadir `NEW_ARRIVALS`.
2. Crear `NewArrivalsCriterion implements RankingCriterion` con su `calculateScore`. *(Si necesita un dato nuevo del producto, añadir antes el VO + campo en `Product`, documento y mappers.)*
3. Registrarlo en `ProductRankingConfiguration` (lista de criterios).
4. `RankingWeightsRequest`: añadir campo `newArrivals` y línea en `ProductRankingRestMapper.toCommand`.
5. Tests: unitario del criterio + caso en `ProductRankingServiceTest` + E2E.

**El algoritmo (`ProductRankingService`) no se toca.** Verbalizar esto: es la demostración del Open/Closed.

### 10.2 "Filtra por categoría / parámetros adicionales"

Añadir el campo a `RankProductsCommand`, extender el puerto (`findByCategory`) y el adapter con una query derivada de Spring Data. La firma del caso de uso no cambia (por eso existe el command).

### 10.3 "Pagina el resultado"

Opción honesta: el ranking es global, así que hay que **puntuar todo y luego cortar** (offset/limit tras ordenar) — válido para tamaño catálogo-categoría. Para escala real: precalcular scores por pesos populares o mover la suma ponderada a una aggregation pipeline de Mongo (proyección computada + sort). Trade-off: lógica de dominio escapando a la BD; lo encapsularía en el adapter como optimización, manteniendo el domain service como fuente de verdad y los tests comparando ambos.

### 10.4 "Cachea el ranking"

El ranking depende de (productos, pesos). Cache por clave = hash de pesos, con invalidación al cambiar productos. Como decorador del caso de uso (`CachingRankProductsService implements RankProductsUseCase`) en infraestructura — bonito porque **aquí sí aplica Decorator**, y puedo contrastarlo con por qué no aplicaba en los criterios.

### 10.5 "Persiste los pesos / configuraciones de ranking con nombre"

Nuevo agregado `RankingConfiguration` (id, nombre, pesos) con su repositorio y CRUD. Los pesos pasan de ser solo input efímero a entidad del dominio.

### 10.6 "Normaliza los scores" (que ventas no domine)

Min-max por criterio dentro del conjunto a rankear: `(x - min) / (max - min)` → todos los criterios en 0–1 y los pesos se vuelven comparables de verdad. Cambio localizado: el domain service necesitaría calcular por criterio antes de ponderar (dos pasadas). Mencionar el edge case max == min.

### 10.7 "Pesos por defecto si el cliente no los manda"

Decisión de producto, no técnica: hoy es 400 deliberado. Si negocio define un default, va en el mapper REST (la API decide el default; el dominio sigue exigiendo pesos explícitos).

### 10.8 "Añade observabilidad / OpenAPI / CI"

- Actuator (health, metrics) + métricas de negocio (tiempo de ranking, tamaño del catálogo).
- springdoc-openapi: anotaciones solo en infraestructura REST.
- GitHub Actions: `./mvnw verify` — Testcontainers funciona en CI con Docker disponible.

---

## 11. Debilidades que me pueden atacar (anticiparse)

| Crítica | Defensa preparada |
| --- | --- |
| "El score con `double` puede dar problemas de precisión" | Valor de ordenación relativo, no dinero; `Double.compare` para ordenar; NaN/Inf rechazados; redondeo solo en frontera. Cambio encapsulado en `Score` si hiciera falta. (§6.2) |
| "Añadir un criterio obliga a tocar el DTO REST" | Trade-off documentado y deliberado: contrato explícito > extensibilidad silenciosa. La API debe evolucionar con revisión. (§6.4) |
| "`findAll()` no escala" | Acotado al caso de uso (categoría). Evolución clara: paginación o scoring en BD. (§10.3) |
| "El seed en producción es raro" | Es demo del enunciado, idempotente, y los tests no dependen de él. En un sistema real sería una migración o no existiría. |
| "No hay seguridad / rate limiting" | Fuera del scope del enunciado; entraría como filtros/config en infraestructura sin tocar dominio. |
| "¿Por qué no records de Java 17 para los VOs?" | Decisión consciente: clases finales con constructor validador y accessors `value()` — control total de la invariante y simetría entre todos los VOs (algunos, como `Stock`, necesitan copia defensiva que en un record queda menos evidente). Con records el `equals/hashCode` saldría gratis; era un intercambio aceptable en ambas direcciones y sé argumentar ambos. |
| "Tests E2E lentos/frágiles" | 8 tests, ~3s, contenedor compartido por clase, datos preparados por test. La pirámide está respetada: 49 unitarios abajo. |

---

## 12. Datos para no quedarse en blanco

- **Endpoint**: `POST /api/products/ranking`, puerto 8080.
- **Orden con 0.7/0.3**: `5, 1, 3, 2, 6, 4` (scores: 465.0, 90.0, 86.0, 65.0, 44.0, 32.1).
- **Orden solo stock (0/1)**: `2, 3, 4, 6, 1, 5` (4 empatados a 100 → desempate por id).
- **71 tests**: 49 dominio + 2 aplicación + 3 web slice + 3 integración + 8 E2E + 6 ArchUnit.
- **Comandos**: `docker compose up -d` → `./mvnw spring-boot:run`; tests: `./mvnw test`.
- Mongo: BD `product_ranking`, colección `products`, imagen `mongo:7`.
- Paquete raíz: `com.backendtools.productranking`.
