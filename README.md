# ScoreCrediticio - Backend

Backend para el registro de solicitantes de crédito y su ficha financiera. Lo usa el asesor comercial desde el frontend en un flujo secuencial en la misma página: primero registra al solicitante, con el `id` devuelto habilita y registra su perfil financiero.

## Stack tecnológico

* Java 25 + Spring Boot 4.1.1 (WebMVC, Data JPA, Validation)
* PostgreSQL + Hibernate (`ddl-auto: update`)
* Gradle (wrapper incluido) + Dockerfile multistage para Render
* Despliegue: Render (Web Service Docker + Postgres). Frontend: React en Vercel.

## Estructura de carpetas (`src/main/java/com/udea/ScoreCrediticio/`)

* `Model/`: entidades JPA. `Solicitante`, `PerfilFinanciero` (relación 1 a 1) y enum `TipoDocumento` (`CC, CE, PASAPORTE, PPT, TI`).
* `DTOs/Request/`: lo que recibe la API. `SolicitanteRequestDTO` (con `@NotBlank/@Email`, validan campos obligatorios), `PerfilFinancieroRequestDTO` (`ingresos, egresos`).
* `DTOs/Response/`: lo que devuelve la API. `SolicitanteResponseDTO` (`id + datos`), `PerfilFinancieroResponseDTO` (`id, solicitanteId, ingresos, egresos, ingresoNetoDisponible`).
* `Mapper/`: convierte `RequestDTO -> Entity` y `Entity -> ResponseDTO`. Sin lógica de negocio.
* `DAOs/`: repositorios Spring Data. `findByTipoDocumentoAndNumeroDocumento` (duplicados) y `findBySolicitanteId` (un perfil por solicitante).
* `Services/`: reglas de negocio. `registrarSolicitante` (rechaza duplicado por tipo+numero con `409`) y `registrarPerfil` (`404` si no existe el solicitante, `409` si ya tiene perfil).
* `Controller/`: solo expone los 2 `POST`. Valida con `@Valid`.
* `Config/`: `CorsConfig`. Permite `localhost:3000`, `localhost:5173` y `https://*.vercel.app` (+ `FRONTEND_URL` en Render).
* `Exceptions/`: `DuplicateResourceException`, `ResourceNotFoundException` y `GlobalExceptionHandler` (`400` validación, `404` no encontrado, `409` duplicado).

## Endpoints disponibles

Base local: `http://localhost:8080` 

### 1. Registrar solicitante
`POST /api/solicitantes`
```json
{
  "tipoDocumento": "CC",
  "numeroDocumento": "12345678",
  "nombre": "Ana",
  "apellido": "Pérez",
  "telefono": "3001234567",
  "email": "ana@test.com"
}
```
* `201` -> devuelve el solicitante con `id`. Guárdalo.
* `400` -> falta un obligatorio o formato malo (email, teléfono 7-10 dígitos).
* `409` -> ya existe ese `tipoDocumento + numeroDocumento`.

### 2. Registrar perfil financiero
`POST /api/solicitantes/{solicitanteId}/perfil-financiero`
```json
{ "ingresos": 2500000.00, "egresos": 1200000.00 }
```
* `201` -> devuelve el perfil con `ingresoNetoDisponible = ingresos - egresos`.
* `400` -> ingresos/egresos nulos o negativos.
* `404` -> el `{solicitanteId}` no existe.
* `409` -> ese solicitante ya tiene perfil.

## Cómo usarlo en el frontend (lógica de negocio)

Flujo secuencial, misma página, sin pedir el `id` al asesor:

1. Form 1 (solicitante) habilitado, Form 2 (perfil) deshabilitado.
2. `POST /api/solicitantes` -> con el `id` de la respuesta habilitas el Form 2.
3. `POST /api/solicitantes/{id}/perfil-financiero` con ese `id` en memoria.
```js
const api = import.meta.env.VITE_API_URL; // URL del backend en Render
const sol = await fetch(`${api}/api/solicitantes`, {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify(datosSolicitante)
}).then(r => r.json());
await fetch(`${api}/api/solicitantes/${sol.id}/perfil-financiero`, {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify({ ingresos, egresos })
});
```

## Tablas

**`solicitante`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (SEQUENCE `hibernate_sequence`) | PK |
| tipo_documento | varchar (`CC, CE, PASAPORTE, PPT, TI`) | parte de la llave lógica de duplicado |
| numero_documento | varchar | parte de la llave lógica de duplicado |
| nombre, apellido, telefono, email | varchar | obligatorios |

**`perfil_financiero`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (misma SEQUENCE) | PK |
| ingresos / egresos | numeric(19,2) | `>= 0` |
| solicitante_id | bigint UNIQUE NOT NULL | FK -> `solicitante.id`, un perfil por solicitante |

> `ingresoNetoDisponible` no es columna, se calcula como `ingresos - egresos`.


