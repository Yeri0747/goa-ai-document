# Guia de estilo y arquitectura - GOA AI Document

Documento normativo para contribuir en `goa-ai-document`.

## Niveles de regla

- `DEBE`: obligatorio.
- `DEBERIA`: recomendado, salvo razon tecnica explicita.
- `PUEDE`: opcional.

## Arquitectura general

Sistema de microservicios con Eureka + API Gateway + servicios Spring Boot.

```text
eureka
  ->
gateway
  ->
microservicios (goa-ai-document, ...)
```

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

## Recursos (HTTP)

- DEBE usar `@RestController` y sufijo `Resource`.
- DEBE delegar logica de negocio al servicio.
- DEBE usar rutas base como constantes (`public static final String ...`).
- DEBE usar inyeccion por constructor (`@RequiredArgsConstructor` o constructor explicito).
- DEBERIA proteger metodos con `@PreAuthorize` usando constantes de `Security` cuando haya autorizacion de roles.
- NO DEBE usar SpEL literal en `@PreAuthorize`.
- DEBERIA validar entrada con `@Valid` y regex de `Validations` si aplica.

## DTOs

- DEBE ubicarse en `resources.dtos`.
- DEBE seguir esta convencion:
  - `XxxResponseDto`: solo salida.
  - `XxxCreationDto`: solo entrada de creacion.
  - `XxxUpdatingDto`: solo entrada de actualizacion.
  - `XxxDto`: entrada/salida, marcando asimetrias con
    `@JsonProperty(access = Access.READ_ONLY)` y
    `@JsonProperty(access = Access.WRITE_ONLY)`.
- DEBE mantener conversion DTO <-> entidad en capa `resources.dtos` (constructores y `toDomain()`).
- NO DEBE mover DTOs a capa `services`.

## Criterios

- DEBE vivir en `services.criteria` si el proyecto necesita consultas parametrizadas.
- DEBERIA usar sufijo `FindCriteria`.
- DEBERIA recibirse en resources via `@ModelAttribute`.
- NO DEBE contener anotaciones de serializacion HTTP.
- Nota: actualmente no hay endpoint de busqueda en `goa-ai-document`, por lo que esta capa no es obligatoria hoy.

## Servicios

- DEBE usar `@Service` y sufijo `Service`.
- DEBE trabajar con entidades (no con DTOs) en la logica de negocio.
- DEBERIA mantener nombres consistentes: `create`, `read`, `update`, `delete`, `find`.
- DEBE lanzar `NotFoundException` en `read/update/delete` cuando no exista recurso.
- DEBERIA encapsular invariantes en metodos privados (`assertXxx`, `validateXxx`, etc.).

## Persistencia (Mongo)

- DEBE usar `MongoRepository`.
- DEBERIA usar `MongoTemplate` y `Criteria` para consultas complejas cuando sea necesario.
- DEBE usar convenciones Spring Data en metodos simples (`findByX`, `existsByX`, etc.).
- Repository custom solo si el requisito exige consultas complejas o filtros dinamicos.

## Entidades

- DEBE ser `@Document` sin sufijo.
- DEBE marcar id con `@Id`.
- DEBERIA usar `@Indexed(unique = true)` en campos unicos.
- PUEDE usar `@DBRef` si la relacion lo requiere.

## Infrastructure support

- DEBE contener utilidades tecnicas internas reutilizables.
- NO DEBE contener logica de negocio.
- Ejemplos: parseo de PDF, generacion UUID, adaptadores de utilidades internas.

## Infrastructure clients

- DEBE contener clientes/adaptadores de salida a servicios externos.
- DEBE aislar detalles de protocolo/integracion (AWS S3, OpenAI, Feign, headers, etc.).
- No mezclar clientes externos con utilidades internas en el mismo paquete.

## Seguridad

- DEBE proteger endpoints con `@PreAuthorize` si el proyecto requiere autorizacion por rol.
- DEBE usar constantes de `Security`.
- DEBERIA mantener visible la regla de autorizacion en cada metodo sensible.
- DEBE mantener coherencia entre las reglas de `@PreAuthorize` y la configuracion de `ResourceServerConfig`.

## Excepciones y errores

- DEBE usar excepciones de dominio cuando aplique.
- DEBE centralizar mapeo HTTP en `resources.httperrors.ApiExceptionHandler`.
- NO DEBE declarar excepciones en paquetes de entidades.

## Inicializadores y seeders

- DEBE implementarse con `ApplicationRunner`.
- DEBE ejecutar logica en `run(...)`.
- `SeederForDev` DEBE limitarse a perfiles `dev` y `test`.
- DEBERIA usar ids fijos cuando los tests dependan de esos ids.

## Tests

Convencion actual:
- Unitarios: `*Test`.
- Integracion: `*IT`.
- Funcionales HTTP: `*FT`.

Reglas:
- DEBE cubrir casos felices y de error.
- DEBE restaurar estado cuando el test muta datos compartidos.
- DEBERIA usar `@WithMockUser` en pruebas de servicio que dependan de rol.

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
- SpEL literal en `@PreAuthorize`.
- Logica de negocio en constructores de beans.
- Mezclar utilidades internas (`support`) con clientes externos (`clients`) en el mismo paquete.
