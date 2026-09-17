# ScoreCrediticio - Backend

Backend para el registro de solicitantes de crédito y su ficha financiera. Cada registro es independiente y se vincula por `tipoDocumento + numeroDocumento` (el frontend nunca maneja el `id` interno).

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
* `Services/`: reglas de negocio. `registrarSolicitante` (rechaza duplicado por tipo+numero con `409`) y `registrarPerfil`/`consultarPorDocumento` (resuelven el solicitante por documento: `404` si no existe, `409` si ya tiene perfil).
* `Controller/`: `SolicitanteController` (`POST /api/solicitantes`) y `PerfilFinancieroController` (`POST + GET /api/perfil-financiero` por documento). Valida con `@Valid/@Validated`.
* `Config/`: `CorsConfig`. Permite `localhost:3000`, `localhost:5173` y `https://*.vercel.app` (+ `FRONTEND_URL` en Render).
* `Exceptions/`: `DuplicateResourceException`, `ResourceNotFoundException` y `GlobalExceptionHandler` (`400` validación, `404` no encontrado, `409` duplicado).

## Endpoints disponibles

Base local: `http://localhost:8080` — Producción: `https://scorecrediticiobackend.onrender.com`

> Nota: al abrir la URL base en el navegador verás `404 Ruta no encontrada`. Es normal, la API no tiene interfaz web. Usa Postman o el frontend. 

### 1. Registrar solicitante
`POST https://scorecrediticiobackend.onrender.com/api/solicitantes`
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
* `201` -> devuelve el solicitante con `id`. 
* `400` -> falta un obligatorio o formato malo (email, teléfono 7-10 dígitos).
* `409` -> ya existe ese `tipoDocumento + numeroDocumento`.

### 2. Registrar perfil financiero 
`POST https://scorecrediticiobackend.onrender.com/api/perfil-financiero`
```json
{
  "tipoDocumento": "CC",
  "numeroDocumento": "12345678",
  "ingresos": 2500000.00,
  "egresos": 1200000.00
}
```
* `201` -> devuelve el perfil con `ingresoNetoDisponible = ingresos - egresos`.
* `400` -> ingresos/egresos nulos o negativos, o documento con formato inválido.
* `404` -> no existe solicitante con ese `tipoDocumento + numeroDocumento`.
* `409` -> ese solicitante ya tiene perfil.

### 3. Consultar ficha financiera (por documento)
`GET https://scorecrediticiobackend.onrender.com/api/perfil-financiero?tipoDocumento=CC&numeroDocumento=12345678`
* `200` -> `id, solicitanteId, tipoDocumento, numeroDocumento, ingresos, egresos, ingresoNetoDisponible`.
* `400` -> falta un query param o `tipoDocumento` inválido (`CC, CE, PASAPORTE, PPT, TI`).
* `404` -> solicitante no existe o aún no tiene perfil financiero registrado.

## Cómo usarlo en el frontend (lógica de negocio)

Dos registros independientes: cada formulario se identifica por `tipoDocumento + numeroDocumento`:

1. Form solicitante -> `POST /api/solicitantes`.
2. Form perfil -> `POST /api/perfil-financiero` con `{tipoDocumento, numeroDocumento, ingresos, egresos}`.
3. Consulta -> `GET /api/perfil-financiero?tipoDocumento=...&numeroDocumento=...`.
```js
const api = import.meta.env.VITE_API_URL; // = https://scorecrediticiobackend.onrender.com
await fetch(`${api}/api/solicitantes`, {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify(datosSolicitante)
});
await fetch(`${api}/api/perfil-financiero`, {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify({ tipoDocumento, numeroDocumento, ingresos, egresos })
});
const ficha = await fetch(
  `${api}/api/perfil-financiero?tipoDocumento=${tipoDocumento}&numeroDocumento=${numeroDocumento}`
).then(r => r.json());
```

## Cómo conectarse al backend (frontend y pruebas)

**Producción:** `https://scorecrediticiobackend.onrender.com`

1. **Postman:** crea una variable `baseUrl` con ese valor. Prueba:
   * `POST {{baseUrl}}/api/solicitantes` con el JSON del punto 1 -> esperas `201` y un `id`.
   * `POST {{baseUrl}}/api/perfil-financiero` con `{tipoDocumento, numeroDocumento, ingresos, egresos}` -> esperas `201`.
   * `GET {{baseUrl}}/api/perfil-financiero?tipoDocumento=CC&numeroDocumento=12345678` -> esperas `200` con la ficha.
   * En plan Free la primera petición puede tardar ~50s (el servicio se duerme). Reintenta si da timeout.
2. **React (Vite) en Vercel:** define la variable de entorno:
   ```
   VITE_API_URL=https://scorecrediticiobackend.onrender.com
   ```
   y usa `${import.meta.env.VITE_API_URL}/api/...` como en el ejemplo de arriba. No pongas la URL fija en el código.
3. **CORS:** ya permite `localhost:3000`, `localhost:5173` y `https://*.vercel.app`. Si el navegador bloquea, revisa que `FRONTEND_URL` en Render tenga tu URL final de Vercel.
4. **BD:** el frontend nunca se conecta directo a Postgres, solo a la API. La BD la gestiona el backend en Render.

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


