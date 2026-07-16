# Condos Admin

Sistema de administracion de condominios basado en microservicios con Spring Boot, MongoDB y Traefik.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        Traefik (API Gateway)                     │
│                    Puerto 80/443 (SSL/TLS)                      │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    Auth API   │   │   Tenant API  │   │   User API    │
│   (Puerto     │   │   (Puerto     │   │   (Puerto     │
│    8080)      │   │    8080)      │   │    8080)      │
└───────────────┘   └───────────────┘   └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │    MongoDB      │
                    │   (Puerto       │
                    │    27017)       │
                    └─────────────────┘
```

## Microservicios

| Servicio    | Descripcion                              | Puerto |
|-------------|------------------------------------------|--------|
| auth-api    | Autenticacion y generacion de JWT        | 8080   |
| tenant-api  | Gestion multi-tenant (organizaciones)   | 8080   |
| user-api    | Gestion de usuarios y perfiles          | 8080   |
| board-api   | Tableros, tareas y archivos adjuntos    | 8080   |
| notify-api  | Notificaciones (futuro)                  | 8080   |

## Requisitos

- Docker Desktop 4.x+
- Docker Compose v2.x+
- Java 17 (para desarrollo local)
- Maven 3.8+ (para desarrollo local)

## Configuracion Rapida

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/condos-admin.git
cd condos-admin
```

### 2. Configurar variables de entorno

```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar las variables sensibles
nano .env
```

**Variables obligatorias:**

| Variable                    | Descripcion                                    |
|-----------------------------|------------------------------------------------|
| `JWT_SECRET`                | Secreto JWT (minimo 64 caracteres)           |
| `MONGO_INITDB_ROOT_USERNAME`| Usuario de MongoDB                             |
| `MONGO_INITDB_ROOT_PASSWORD`| Password de MongoDB                           |
| `MINIO_ROOT_USER`           | Usuario de MinIO                              |
| `MINIO_ROOT_PASSWORD`       | Password de MinIO                             |

**Generar JWT_SECRET seguro:**
```bash
openssl rand -base64 64 | tr -d '\n'
```

### 3. Construir y levantar servicios

```bash
# Construir los microservicios
./mvnw clean package -DskipTests

# Levantar con Docker Compose
docker compose up -d
```

### 4. Verificar que todo funciona

```bash
# Verificar estado de los contenedores
docker compose ps

# Verificar salud de auth-api
curl http://localhost:8088/actuator/health
```

## Desarrollo Local

### Prerequisitos

1. MongoDB corriendo localmente o via Docker
2. Java 17+ instalado

### Ejecutar un microservicio individualmente

```bash
# Configurar variables de entorno
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/condos"
export JWT_SECRET="tu-secreto-seguro"
export SPRING_PROFILES_ACTIVE=local

# Ejecutar auth-api
cd auth-api
../mvnw spring-boot:run
```

### Ejecutar todos los servicios con Docker

```bash
docker compose up -d
```

## Estructura del Proyecto

```
condos-admin/
├── auth-api/              # Servicio de autenticacion
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── tenant-api/            # Gestion de organizaciones
├── user-api/              # Gestion de usuarios
├── board-api/             # Tableros y tareas
├── notify-api/            # Notificaciones (futuro)
├── shared-lib/            # Libreria compartida (JWT, DTOs)
├── gateway/               # Configuracion de Traefik
├── docker-compose.yml     # Orquestacion de contenedores
├── .env.example           # Variables de entorno (plantilla)
└── pom.xml                # Maven parent
```

## API Endpoints

### Auth API

| Metodo | Endpoint                | Descripcion                    |
|--------|-------------------------|--------------------------------|
| POST   | `/condos/api/auth/login`| Iniciar sesion                |
| GET    | `/condos/api/auth/me`   | Obtener usuario actual        |
| POST   | `/condos/api/auth/register`| Registrar nuevo usuario    |

### Tenant API

| Metodo | Endpoint                    | Descripcion              |
|--------|-----------------------------|--------------------------|
| GET    | `/condos/api/tenant`        | Listar organizaciones   |
| POST   | `/condos/api/tenant`        | Crear organizacion      |
| PUT    | `/condos/api/tenant/{id}`   | Actualizar organizacion |

### User API

| Metodo | Endpoint                 | Descripcion              |
|--------|--------------------------|--------------------------|
| GET    | `/condos/api/user/me`    | Perfil del usuario       |
| PUT    | `/condos/api/user/me`    | Actualizar perfil        |

## Seguridad

### Buenas Practicas Implementadas

- **JWT**: Tokens con expiracion configurable
- **Variables de entorno**: Sin credenciales en el codigo
- **HTTPS**: Redireccion automatica HTTP → HTTPS
- **CORS**: Configuracion por whitelist
- **Actuator**: Health checks sin informacion sensible

### Produccion

Antes de desplegar en produccion:

1. Cambiar todos los secretos por valores seguros
2. Configurar SSL con Let's Encrypt
3. Habilitar rate limiting
4. Configurar backups de MongoDB
5. Revisar los logs de auditoria

## Testing

```bash
# Ejecutar tests unitarios
./mvnw test

# Ejecutar tests de integracion
./mvnw verify
```

## Contribucion

1. Fork del repositorio
2. Crear rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit de cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## Licencia

Este proyecto es privado y confidencial.

## Soporte

Para reportar problemas o solicitar ayuda, crear un issue en el repositorio.