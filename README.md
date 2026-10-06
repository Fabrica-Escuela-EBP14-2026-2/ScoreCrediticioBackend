# ScoreCrediticio - Backend

Backend para el registro de solicitantes de crédito y su ficha financiera, más el catálogo de variables de riesgo que usa el Administrador para la fórmula del score. Cada registro es independiente y se vincula por `tipoDocumento + numeroDocumento` (el frontend nunca maneja el `id` interno).

## Stack tecnológico

* Java 25 + Spring Boot 4.1.1 (WebMVC, Data JPA, Validation)
* Seguridad: Spring Security + JWT (`io.jsonwebtoken` 0.13.0) + BCrypt
* PostgreSQL + Hibernate (`ddl-auto: update` en local, `validate` en producción). En producción las tablas se crean con los scripts de `sql/`, no con Hibernate.
* Gradle (wrapper incluido) + Dockerfile multistage para Render
* Despliegue: Render (Web Service Docker + Postgres). Frontend: React en Vercel.

## Estructura de carpetas (`src/main/java/com/udea/ScoreCrediticio/`)

* `Model/`: entidades JPA. `Solicitante`, `PerfilFinanciero` (relación 1 a 1), `Usuario`, `VariableRiesgo` y los enums `TipoDocumento` (`CC, CE, PASAPORTE, PPT, TI`), `TipoUsuario` (`ANALISTA, ADMINISTRADOR`), `TipoDatoVariable` (`NUMERICO, PORCENTAJE, CATEGORICO`) y `EstadoVariable` (`Activa, Inactiva`).
* `DTOs/Request/`: lo que recibe la API. `SolicitanteRequestDTO` (con `@NotBlank/@Email`, validan campos obligatorios), `PerfilFinancieroRequestDTO` (`ingresos, egresos`), `LoginRequestDTO` (`email, password`) y `VariableRiesgoRequestDTO` (`nombre, tipoDato, peso`; el peso debe ser `> 0` y `<= 100`).
* `DTOs/Response/`: lo que devuelve la API. `SolicitanteResponseDTO` (`id + datos`), `PerfilFinancieroResponseDTO` (`id, solicitanteId, ingresos, egresos, ingresoNetoDisponible`), `LoginResponseDTO` (`token, tipoToken, expiraEn, email, rol`) y `VariableRiesgoResponseDTO` (`id, nombre, tipoDato, peso, estado, fechaRegistro`).
* `Mapper/`: convierte `RequestDTO -> Entity` y `Entity -> ResponseDTO`. Sin lógica de negocio.
* `DAOs/`: repositorios Spring Data. `findByTipoDocumentoAndNumeroDocumento` (duplicados), `findBySolicitanteId` (un perfil por solicitante), `existsByNombreIgnoreCase` (variables de riesgo) y `sumarPesosPorEstado` (total de pesos activos para validar el 100%).
* `Services/`: reglas de negocio. `registrarSolicitante` (rechaza duplicado por tipo+numero con `409`), `registrarPerfil`/`consultarPorDocumento` (resuelven el solicitante por documento: `404` si no existe, `409` si ya tiene perfil), `AuthService` (valida credenciales con BCrypt y emite el JWT) y `VariableRiesgoService` (`registrarVariable` rechaza nombres duplicados y no deja que la suma de los pesos activos supere el 100%; la transacción va en `SERIALIZABLE` para que dos altas concurrentes no se cuelguen entre sí).
* `Controller/`: `SolicitanteController` (`POST /api/solicitantes`), `PerfilFinancieroController` (`POST + GET /api/perfil-financiero` por documento), `AuthController` (`POST /api/auth/login`) y `VariableRiesgoController` (`POST + GET /api/variables`). Valida con `@Valid/@Validated`.
* `Config/`: `CorsConfig` (permite `localhost:3000`, `localhost:5173` y `https://*.vercel.app`, + `FRONTEND_URL` en Render) y `DataInitializer` (crea el admin y el analista semilla si no existen).
* `Security/`: `JwtService` (firma y lee tokens), `JwtAuthenticationFilter` (lee el header Bearer y autentica la petición), `SecurityConfig` (reglas por rol + bean BCrypt), `RestAuthenticationEntryPoint` (`401`) y `RestAccessDeniedHandler` (`403`).
* `Exceptions/`: `DuplicateResourceException`, `PesoTotalExcedeLimiteException`, `ResourceNotFoundException`, `CredencialesInvalidasException` y `GlobalExceptionHandler` (`400` validación, `401` credenciales, `404` no encontrado, `409` duplicado / suma de pesos excedida / conflicto de concurrencia).

## Endpoints disponibles

Base local: `http://localhost:8080` — Producción: `https://scorecrediticiobackend.onrender.com`

> Nota: la API no tiene interfaz web. Todas las rutas (excepto `POST /api/auth/login`) exigen el header `Authorization: Bearer <token>`; sin un token válido responden `401`. Usa Postman o el frontend.

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

### 4. Registrar variable de riesgo (solo `ADMINISTRADOR`)
`POST https://scorecrediticiobackend.onrender.com/api/variables`
```json
{
  "nombre": "IngresosMensuales",
  "tipoDato": "NUMERICO",
  "peso": 40.0000
}
```
* `201` -> devuelve la variable con `id`, `estado: "Activa"` y `fechaRegistro`. Queda disponible para la fórmula del score.
* `400` -> falta `nombre`/`tipoDato`/`peso`, el peso es `0`, negativo o mayor que `100`, o el `tipoDato` no es `NUMERICO`, `PORCENTAJE` ni `CATEGORICO`.
* `401` -> sin token válido.
* `403` -> el rol es `ANALISTA` (este módulo es solo del `ADMINISTRADOR`).
* `409` -> ya existe una variable con ese nombre (ignora mayúsculas/minúsculas) **o** la suma de los pesos de las variables activas superaría el `100%`. En el segundo caso el cuerpo trae `pesoTotalActual`, `pesoSolicitado` y `pesoTotalResultante` para que el formulario muestre el detalle.

`GET https://scorecrediticiobackend.onrender.com/api/variables`
* `200` -> lista todas las variables (activas e inactivas) ordenadas por nombre. También solo `ADMINISTRADOR`.

### 5. Autenticación

> La única ruta pública es `POST /api/auth/login`. El resto responde `401` sin token válido y `403` si el rol no tiene permiso.

### Iniciar sesión

`POST /api/auth/login`
```json
{
  "email": "admin@score.local",
  "password": "Admin123*"
}
```
* `200` -> `{ token, tipoToken: "Bearer", expiraEn, email, rol }`. `expiraEn` es la vigencia en milisegundos (1 hora).
* `400` -> email/contraseña vacíos o email con formato inválido.
* `401` -> `Usuario o contraseña incorrectos. Por favor intente nuevamente` (el mismo mensaje si el usuario no existe o si la contraseña es incorrecta).

En las demás peticiones envía el token en el header:

```
Authorization: Bearer <token>
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@score.local","password":"Admin123*"}'
```

### Flujo del token

1. El login valida el email y compara la contraseña con BCrypt (`passwordEncoder.matches`).
2. Si son válidas, `JwtService` firma un token HS256 con `email` (subject), `rol` (claim) y expiración de 1 hora.
3. El frontend guarda el token y lo envía en cada petición: `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` valida la firma y vigencia, y autentica la petición con la autoridad `ROLE_<rol>`; `SecurityConfig` decide si la ruta está permitida.

### Usuarios semilla

Al arrancar, `Config/DataInitializer` crea el administrador y el analista **solo si su email no existe**: no se duplican ni se actualizan en arranques posteriores. Las credenciales se toman de `ADMIN_EMAIL`/`ADMIN_PASSWORD` y `ANALISTA_EMAIL`/`ANALISTA_PASSWORD`.

### Roles

* `ANALISTA`: podrá usar los módulos de negocio (solicitantes, perfil financiero).
* `ADMINISTRADOR`: todo lo anterior + módulos de administración (`/api/variables/**`, `/api/formula/**`, `/api/rangos/**`).

> Las rutas de administración se protegen por el prefijo `/api/variables/**` en `SecurityConfig`. Si creas un endpoint de este módulo con otra ruta (por ejemplo `/api/variables-riesgo`), **queda fuera** de la regla y solo exigiría estar autenticado: cualquier `ANALISTA` podría llamarlo.


## Cómo usarlo en el frontend (lógica de negocio)

Primero inicia sesión en `POST /api/auth/login`, guarda el `token` y envíalo como `Authorization: Bearer <token>` en cada petición. Usa el `rol` devuelto para mostrar u ocultar los módulos de administración (solo `ADMINISTRADOR`) y redirige al login cuando recibas `401`.

Dos registros independientes: cada formulario se identifica por `tipoDocumento + numeroDocumento`:

1. Form solicitante -> `POST /api/solicitantes`.
2. Form perfil -> `POST /api/perfil-financiero` con `{tipoDocumento, numeroDocumento, ingresos, egresos}`.
3. Consulta -> `GET /api/perfil-financiero?tipoDocumento=...&numeroDocumento=...`.

Módulo de variables (solo `ADMINISTRADOR`):

4. Alta de variable -> `POST /api/variables` con `{nombre, tipoDato, peso}`. Si el peso hace pasar la suma de activas del `100%`, el backend responde `409` con `pesoTotalActual`, `pesoSolicitado` y `pesoTotalResultante`: muéstralos en el formulario sin limpiarlo.
```js
const api = import.meta.env.VITE_API_URL; // = https://scorecrediticiobackend.onrender.com

const sesion = await fetch(`${api}/api/auth/login`, {
  method: 'POST', headers: {'Content-Type':'application/json'},
  body: JSON.stringify({ email, password })
}).then(r => r.json());

const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${sesion.token}`
};

await fetch(`${api}/api/solicitantes`, {
  method: 'POST', headers,
  body: JSON.stringify(datosSolicitante)
});
await fetch(`${api}/api/perfil-financiero`, {
  method: 'POST', headers,
  body: JSON.stringify({ tipoDocumento, numeroDocumento, ingresos, egresos })
});
const ficha = await fetch(
  `${api}/api/perfil-financiero?tipoDocumento=${tipoDocumento}&numeroDocumento=${numeroDocumento}`,
  { headers }
).then(r => r.json());

// Solo ADMINISTRADOR
const alta = await fetch(`${api}/api/variables`, {
  method: 'POST', headers,
  body: JSON.stringify({ nombre, tipoDato, peso })
});
if (alta.status === 409) {
  const error = await alta.json(); // pesoTotalActual / pesoSolicitado / pesoTotalResultante
}
```

## Cómo conectarse al backend (frontend y pruebas)

**Producción:** `https://scorecrediticiobackend.onrender.com`

1. **Postman:** crea una variable `baseUrl` con ese valor. Prueba:
   * `POST {{baseUrl}}/api/auth/login` con las credenciales reales (en producción son las creadas en el primer arranque, no necesariamente las de desarrollo `admin@score.local`/`Admin123*`) -> esperas `200` con `token` y `rol`. Guarda el token en una variable de colección y configúrala como Bearer Token.
   * `POST {{baseUrl}}/api/solicitantes` con el JSON del punto 1 -> esperas `201` y un `id`.
   * `POST {{baseUrl}}/api/perfil-financiero` con `{tipoDocumento, numeroDocumento, ingresos, egresos}` -> esperas `201`.
   * `GET {{baseUrl}}/api/perfil-financiero?tipoDocumento=CC&numeroDocumento=12345678` -> esperas `200` con la ficha.
   * `POST {{baseUrl}}/api/variables` (con el token del **admin**) con `{"nombre":"Ingresos","tipoDato":"NUMERICO","peso":40}` -> esperas `201`; si mandas otro `40` sin ajustar, esperas `409` con el detalle de la suma.
   * Sin token (o con token vencido) estas rutas responden `401`; con rol `ANALISTA` en rutas admin responden `403`.
   * En plan Free la primera petición puede tardar ~50s (el servicio se duerme). Reintenta si da timeout.
2. **React (Vite) en Vercel:** define la variable de entorno:
   ```
   VITE_API_URL=https://scorecrediticiobackend.onrender.com
   ```
   y usa `${import.meta.env.VITE_API_URL}/api/...` como en el ejemplo de arriba. No pongas la URL fija en el código.
3. **CORS:** ya permite `localhost:3000`, `localhost:5173` y `https://*.vercel.app`. Si el navegador bloquea, revisa que `FRONTEND_URL` en Render tenga tu URL final de Vercel.
4. **BD:** el frontend nunca se conecta directo a Postgres, solo a la API. La BD la gestiona el backend en Render.

## Tablas

**`usuario`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (secuencia `usuario_seq`) | PK |
| email | varchar(255) | UNIQUE NOT NULL, identificador del login |
| password | varchar(255) | hash BCrypt, nunca texto plano |
| tipo_usuario | varchar(20) | `ANALISTA` o `ADMINISTRADOR` (`@Enumerated(EnumType.STRING)`) |


**`solicitante`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (secuencia `solicitante_seq`) | PK |
| tipo_documento | varchar (`CC, CE, PASAPORTE, PPT, TI`) | parte de la llave lógica de duplicado |
| numero_documento | varchar | parte de la llave lógica de duplicado |
| nombre, apellido, telefono, email | varchar | obligatorios |

**`perfil_financiero`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (secuencia propia `perfil_financiero_seq`) | PK |
| ingresos / egresos | numeric(19,2) | `>= 0` |
| solicitante_id | bigint UNIQUE NOT NULL | FK -> `solicitante.id`, un perfil por solicitante |

> `ingresoNetoDisponible` no es columna, se calcula como `ingresos - egresos`.

> Cada entidad tiene su propia secuencia (`usuario_seq`, `solicitante_seq`, `perfil_financiero_seq`, `variable_riesgo_seq`) porque Hibernate 7 las genera así con `GenerationType.SEQUENCE`.

**`variable_riesgo`**
| columna | tipo | nota |
|---|---|---|
| id | bigint (secuencia `variable_riesgo_seq`) | PK |
| nombre | varchar(100) UNIQUE NOT NULL | el backend además lo compara sin distinguir mayúsculas |
| tipo_dato | varchar(20) NOT NULL | `NUMERICO`, `PORCENTAJE` o `CATEGORICO` (`@Enumerated(EnumType.STRING)`) |
| peso | numeric(7,4) NOT NULL | `> 0` y `<= 100`; la suma de las activas no puede pasar de `100` |
| estado | varchar(20) NOT NULL | `ACTIVA` o `INACTIVA`; sale como `"Activa"`/`"Inactiva"` en el JSON |
| fecha_registro | timestamp(6) NOT NULL | la assigns Hibernate (`@CreationTimestamp`) |

