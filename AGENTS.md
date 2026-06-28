# Guia de estilo y arquitectura - GOA AI Document

Documento normativo para contribuir en `goa-ai-document`.

## Niveles de regla

- `DEBE`: obligatorio.
- `DEBERIA`: recomendado, salvo razon tecnica explicita.
- `PUEDE`: opcional.

## Arquitectura general

`goa-ai-document` es un microservicio Spring Boot del ecosistema GOA, responsable
de las capacidades de Document AI: subida de PDF, clasificación, resumen e
extracción de datos de facturas.

### Integración con el cliente

En el módulo Document AI de `goa-front`, las peticiones se dirigen **directamente**
a este microservicio mediante la URL configurada en `REST_AI_DOCUMENT`
(p. ej. `http://localhost:8091` en desarrollo). **No** enrutan por el API Gateway
ni dependen del registro en Eureka.

```text
goa-front (Document AI)
  ->
goa-ai-document (/document-ai/...)
  ->
MongoDB | Amazon S3 | OpenAI | AWS Textract
```

### Contexto del ecosistema GOA

El resto de módulos de GOA siguen un esquema de microservicios con API Gateway
y registro dinámico (Eureka). Ese patrón **no forma parte del despliegue actual**
de Document AI, aunque `goa-ai-document` comparte stack y convenciones con el
resto de servicios Spring Boot del proyecto.

```text
[eureka -> gateway -> otros microservicios GOA]   (fuera del alcance de Document AI)
```

### Organización interna

El código sigue una arquitectura en capas (`resources` -> `services` ->
`data` / `infrastructure`). La estructura de paquetes se detalla en la sección
siguiente.

## Alcance actual de Document AI

Este microservicio expone **acciones de dominio**, no un CRUD REST completo:

| Operación | Endpoint (resumen) |
|-----------|-------------------|
| Subir PDF | `POST /document-ai/documents` |
| Resumir documento | `POST /document-ai/documents/{id}/summary` |
| Extraer factura | `GET /document-ai/documents/{id}/invoice` |

Implicaciones para la guía:

- La entrada principal es **multipart** (`MultipartFile`), no DTOs JSON de creación.
- Los DTOs actuales son solo de **salida** (`*ResponseDto`).
- No hay endpoint de **búsqueda** parametrizada → no se usa capa `criteria`.
- En el **despliegue actual**, los endpoints `/document-ai/documents/**` son
  **públicos** (`permitAll` en `ResourceServerConfig`). Las reglas de
  `@PreAuthorize` solo aplican si ese despliegue cambia.

## Estructura real del microservicio

```text
es.upm.api/
  configurations/
  data/
    daos/
    entities/
  exceptions/
  infrastructure/
    support/
    clients/
  resources/
    dtos/
    httperrors/
  services/
```

Notas:
- No existe paquete `integrations/`; los clientes externos deben ubicarse en `infrastructure.clients`.
- `infrastructure.support` contiene utilidades tecnicas reutilizables por servicios.
- El seeder de desarrollo vive en `configurations` como `DatabaseSeederDev`.

## Recursos (HTTP)

- DEBE usar `@RestController` y sufijo `Resource`.
- DEBE delegar logica de negocio al servicio (salvo endpoints de sistema como `/system`, donde la lógica de presentación puede permanecer en el resource).
- DEBE usar rutas base como constantes (`public static final String ...`).
- DEBE usar inyeccion por constructor (`@RequiredArgsConstructor` o constructor explicito).
- DEBERIA validar entrada con `@Valid` cuando el endpoint reciba un DTO JSON.
  Para multipart (`MultipartFile`, `@RequestParam`), validar en el servicio o con
  anotaciones de validacion propias.

## DTOs

- DEBE ubicarse en `resources.dtos`.
- Hoy el API solo expone DTOs de salida (`*ResponseDto`). Cuando existan endpoints
  con body JSON, aplicar ademas:
  - `XxxCreationDto`: solo entrada de creacion.
  - `XxxUpdatingDto`: solo entrada de actualizacion.
  - `XxxDto`: entrada/salida, marcando asimetrias con
    `@JsonProperty(access = Access.READ_ONLY)` y
    `@JsonProperty(access = Access.WRITE_ONLY)`.
- DEBE mantener conversion DTO <-> entidad en capa `resources.dtos`:
  - constructores desde entidad para `*ResponseDto`;
  - `toDomain()` cuando exista DTO de entrada.
- NO DEBE mover DTOs a capa `services`.

## Servicios

- DEBE usar `@Service` y sufijo `Service`.
- DEBE trabajar con entidades (no con DTOs) en la logica de negocio.
- DEBERIA usar nombres que describan la operacion de dominio (`uploadDocument`,
  `summarizeDocument`, `extractInvoice`, etc.). El patron `create`, `read`,
  `update`, `delete`, `find` solo aplica si el endpoint es CRUD.
- DEBE lanzar `NotFoundException` cuando la operacion requiere un recurso
  persistido que no existe.
- DEBERIA encapsular invariantes en metodos privados (`assertXxx`, `validateXxx`, etc.).

## Persistencia (Mongo)

- DEBE usar `MongoRepository`.
- DEBERIA usar `MongoTemplate` y `Criteria` para consultas complejas cuando sea necesario.
- DEBE usar convenciones Spring Data en metodos simples (`findByX`, `existsByX`, etc.).
- Repository custom solo si el requisito exige consultas complejas o filtros dinamicos.

## Entidades

- DEBE ser `@Document` sin sufijo.
- DEBE marcar id con `@Id`.
- PUEDE usar `@DBRef` si la relacion lo requiere.

## Infrastructure support

- DEBE contener utilidades tecnicas internas reutilizables.
- NO DEBE contener logica de negocio.
- Ejemplos: parseo de PDF, generacion UUID, adaptadores de utilidades internas.

## Infrastructure clients

- DEBE contener clientes/adaptadores de salida a servicios externos.
- DEBE aislar detalles de protocolo/integracion (AWS SDK, OpenAI, RestClient, headers, etc.).
- No mezclar clientes externos con utilidades internas en el mismo paquete.

## Seguridad

### Despliegue actual

Los endpoints de Document AI (`/document-ai/documents/**`) son **publicos** en
`ResourceServerConfig`. No se exige `@PreAuthorize` ni token JWT para consumirlos
desde `goa-front`.

### Si el despliegue requiere autorizacion por rol

- DEBE proteger endpoints con `@PreAuthorize`.
- DEBE usar constantes de `Security` (no SpEL literal en la anotacion).
- DEBERIA mantener visible la regla de autorizacion en cada metodo sensible.
- DEBE mantener coherencia entre las reglas de `@PreAuthorize` y la configuracion de `ResourceServerConfig`.

## Excepciones y errores

- DEBE usar excepciones de dominio cuando aplique.
- DEBE ubicarlas en `exceptions` (p. ej. `NotFoundException`, `BadRequestException`).
- DEBE centralizar mapeo HTTP en `resources.httperrors.ApiExceptionHandler`.
- NO DEBE declarar excepciones en paquetes de entidades.

## Inicializadores y seeders

- DEBE implementarse en `configurations` con la clase `DatabaseSeederDev`.
- DEBE limitarse a perfiles `dev` y `test` (`@Profile({"dev", "test"})`).
- DEBE ejecutar la logica de carga en un metodo invocado desde `@PostConstruct` (p. ej. `init()`).
- DEBERIA usar ids fijos como constantes publicas cuando los tests dependan de esos ids
  (p. ej. `DOC_ID_1`, `DOC_ID_2`).

## Tests

Convencion actual:
- Unitarios: `*Test`.
- Integracion: `*IT`.
- Funcionales HTTP: `*FT`.

Reglas:
- DEBE cubrir casos felices y de error.
- DEBE restaurar estado cuando el test muta datos compartidos.
- DEBERIA usar `@WithMockUser` solo en pruebas que ejercen autorizacion por rol
  (p. ej. tests HTTP cuando los endpoints esten protegidos). No es necesario en
  tests de servicio si la logica no depende del contexto de seguridad.

## Tecnologia y build

- Java objetivo del proyecto: **21**.
- Spring Boot: `3.5.x`.
- Spring Cloud: `2025.0.x`.
- Lombok: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Log4j2`.
- Conversion DTO <-> entidad con `BeanUtils.copyProperties` y builders.

Regla de entorno:
- DEBE compilarse con JDK 21 para evitar problemas de annotation processing.
- Si se usa JDK 23+, DEBERIA activarse annotation processing de forma explicita.

## Antipatrones prohibidos

- DTOs en capa `services`.
- Exponer entidades directamente desde resources.
- Logica de negocio en constructores de beans.
- Mezclar utilidades internas (`support`) con clientes externos (`clients`) en el mismo paquete.
