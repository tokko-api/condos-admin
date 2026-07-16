# Guia de Despliegue - DigitalOcean

Este documento describe como desplegar el sistema condos-admin en un Droplet de DigitalOcean.

## Requisitos del Servidor

### Especificaciones Minimas

| Recurso      | Minimo | Recomendado |
|--------------|--------|-------------|
| CPU          | 2 vCPU | 4 vCPU      |
| RAM          | 4 GB   | 8 GB        |
| Almacenamiento| 50 GB | 100 GB      |
| SO           | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |

### Servicios Necesarios

- Docker
- Docker Compose v2
- Firewall (ufw)

## Preparacion del Servidor

### 1. Actualizar el sistema

```bash
# Conectar al servidor
ssh root@tu-droplet-ip

# Actualizar paquetes
apt update && apt upgrade -y

# Instalar herramientas basicas
apt install -y curl wget git
```

### 2. Instalar Docker

```bash
# Instalar Docker
curl -fsSL https://get.docker.com | sh

# Agregar usuario al grupo docker (opcional)
usermod -aG docker $USER

# Verificar instalacion
docker --version
docker compose version
```

### 3. Configurar Firewall

```bash
# Habilitar puertos necesarios
ufw allow 22/tcp      # SSH
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS

# Habilitar firewall
ufw enable

# Verificar estado
ufw status
```

### 4. Crear usuario de aplicacion (recomendado)

```bash
# Crear usuario para la aplicacion
adduser condosadmin
usermod -aG docker condosadmin
usermod -aG sudo condosadmin

# Cambiar a este usuario
su - condosadmin
```

## Despliegue de la Aplicacion

### 1. Clonar el repositorio

```bash
# Crear directorio
mkdir -p /opt/condos-admin
cd /opt/condos-admin

# Clonar repositorio
git clone https://github.com/tokko-api/condos-admin.git .

# O copiar archivos via scp
# scp -r ./condos-admin condosadmin@tu-droplet:/opt/
```

### 2. Configurar variables de entorno

```bash
# Crear archivo .env
cp .env.example .env
nano .env
```

**Configuracion de produccion (.env):**

```env
# Proyecto
PROJECT_NAME=condos
NETWORK_NAME=condos-admin-net

# MongoDB (CAMBIAR ESTOS VALORES)
MONGO_INITDB_ROOT_USERNAME=condos_admin_prod
MONGO_INITDB_ROOT_PASSWORD=<generar-password-seguro>
MONGO_INITDB_DATABASE=condos
MONGO_PORT=27017

# Usuario de aplicacion creado por mongo_init/init.js al primer arranque
# (debe coincidir con el usuario/password usados en SPRING_DATA_MONGODB_URI abajo)
APP_MONGO_USER=<generar-usuario-seguro>
APP_MONGO_PASSWORD=<generar-password-seguro>

# URI que usan los microservicios. Usa APP_MONGO_USER/APP_MONGO_PASSWORD y
# authSource=condos (init.js crea ese usuario en la DB "condos", no en admin).
SPRING_DATA_MONGODB_URI=mongodb://<APP_MONGO_USER>:<APP_MONGO_PASSWORD>@mongo:27017/condos?authSource=condos

# JWT (CAMBIAR ESTOS VALORES)
JWT_SECRET=<generar-secreto-64-caracteres>
JWT_EXPIRES_MINUTES=240

# MinIO (CAMBIAR ESTOS VALORES)
MINIO_ROOT_USER=<generar-usuario-seguro>
MINIO_ROOT_PASSWORD=<generar-password-seguro>
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001

# Perfil de Spring
SPRING_PROFILES_ACTIVE=prod

# Email para Let's Encrypt
ACME_EMAIL=tu-email@dominio.com

# URLs publicas
PUBLIC_API_BASE=https://api.tu-dominio.com
```

**Generar secretos seguros:**

```bash
# JWT_SECRET (64 caracteres)
openssl rand -base64 64 | tr -d '\n'

# Password MongoDB
openssl rand -base64 32 | tr -d '\n'

# MinIO credentials
openssl rand -hex 16
```

### 3. Configurar dominio

Editar `gateway/traefik_dynamic.yml` con tu dominio:

```yaml
http:
  routers:
    r-auth:
      rule: "Host(`api.tu-dominio.com`) && PathPrefix(`/condos/api/auth`)"
      # ... resto de configuracion
```

### 4. Construir y levantar servicios

> Los `Dockerfile` son **multi-stage**: compilan cada microservicio con Maven
> dentro de la imagen (etapa `build`) y solo dejan el `.jar` en la imagen final.
> Por eso en el Droplet **solo necesitas Docker** — no hace falta instalar Maven
> ni el JDK, ni subir los `target/`. El primer `build` tarda (descarga Maven +
> dependencias); los siguientes usan cache.

```bash
# Construir imagenes (compila dentro de Docker)
docker compose build

# Levantar servicios
docker compose up -d

# Verificar estado
docker compose ps
docker compose logs -f
```

### 5. Verificar salud de los servicios

```bash
# Verificar MongoDB
docker compose exec mongo mongosh -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD --eval "db.adminCommand('ping')"

# Verificar auth-api
curl http://localhost:8080/actuator/health

# Verificar conectividad
curl https://api.tu-dominio.com/condos/api/auth/ping
```

## Configuracion de Dominio y SSL

### 1. Configurar DNS

En tu proveedor de DNS (Cloudflare, Namecheap, etc.):

| Tipo  | Nombre          | Valor              |
|-------|-----------------|--------------------|
| A     | api             | IP_DEL_DROPLET     |
| A     | @               | IP_DEL_DROPLET     |
| CNAME | www             | tu-dominio.com     |

### 2. Let's Encrypt (automatico)

Traefik configura automaticamente SSL con Let's Encrypt. El certificado se guarda en `acme.json`.

```bash
# Crear archivo de certificados
touch acme.json
chmod 600 acme.json
```

### 3. Verificar SSL

```bash
# Verificar certificado
curl -vI https://api.tu-dominio.com 2>&1 | grep "SSL certificate"
```

## Mantenimiento

### Logs

```bash
# Ver logs de todos los servicios
docker compose logs -f

# Ver logs de un servicio especifico
docker compose logs -f auth-api

# Ver logs en tiempo real
docker compose logs -f --tail=100
```

### Backups

```bash
# Backup de MongoDB
docker compose exec mongo mongodump --archive=/tmp/backup.archive \
  -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD

# Copiar backup al host
docker compose cp mongo:/tmp/backup.archive ./backups/$(date +%Y%m%d_%H%M%S).archive

# Backup de MinIO
docker compose exec minio mc mirror local/ /tmp/minio-backup/
```

### Actualizacion

```bash
# Detener servicios
docker compose down

# Actualizar codigo
git pull origin main

# Reconstruir y levantar
docker compose build --no-cache
docker compose up -d
```

### Rollback

```bash
# Volver a version anterior
git checkout <commit-hash>
docker compose build
docker compose up -d
```

## Monitoreo

### Health Checks

```bash
# Verificar todos los servicios
for service in auth-api tenant-api user-api board-api; do
  echo "Checking $service..."
  docker compose exec $service curl -s http://localhost:8080/actuator/health
done
```

### Recursos

```bash
# Uso de recursos
docker stats

# Espacio en disco
df -h

# Memoria
free -h
```

## Troubleshooting

### Los contenedores no inician

```bash
# Verificar logs
docker compose logs auth-api

# Verificar variables de entorno
docker compose config

# Verificar red
docker network ls
docker network inspect condos-admin-net
```

### Error de conexion a MongoDB

```bash
# Verificar que MongoDB esta corriendo
docker compose ps mongo

# Verificar credenciales
docker compose exec mongo mongosh -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD

# Verificar URI de conexion
echo $SPRING_DATA_MONGODB_URI
```

### Error de SSL/Certificado

```bash
# Verificar que acme.json tiene permisos correctos
ls -la acme.json

# Verificar logs de Traefik
docker compose logs traefik

# Forzar renovacion de certificado
rm acme.json && docker compose restart traefik
```

## Seguridad Adicional

### 1. Puertos de MongoDB y MinIO (ya no se exponen por defecto)

El `docker-compose.yml` de este repo **ya no publica** los puertos de Mongo (27017)
ni de MinIO (9000/9001) al host. Docker manipula `iptables` directamente y suele
**evitar las reglas de `ufw`**, asi que publicarlos expondria estos servicios a
internet aunque el firewall diga lo contrario. Ambos servicios solo se acceden
por la red interna de Docker (`mongo:27017`, `minio:9000`).

Si necesitas acceso puntual para debug, usa un tunel SSH en vez de exponer el puerto:

```bash
ssh -L 27017:localhost:27017 usuario@tu-droplet   # Mongo
ssh -L 9001:localhost:9001 usuario@tu-droplet     # Consola MinIO
```

### 2. notify-api no se despliega todavia

El modulo `notify-api` en este repo es actualmente una copia duplicada de
`auth-api` (mismo `artifactId` en su `pom.xml`, mismos paquetes `com.condos.auth.*`).
No esta incluido en `docker-compose.yml` hasta aclarar que debia contener
realmente. No lo agregues al despliegue sin revisar/reescribir su codigo primero.

### 2. Configurar fail2ban (opcional)

```bash
apt install fail2ban
systemctl enable fail2ban
systemctl start fail2ban
```

### 3. Actualizaciones automaticas

```bash
# Instalar unattended-upgrades
apt install unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades
```

## Costos Estimados (DigitalOcean)

| Recurso       | Costo Mensual |
|---------------|---------------|
| Droplet 4GB   | ~$24/mes      |
| Backup        | ~$4.80/mes    |
| Dominio       | ~$12/year     |
| **Total**     | ~$29/mes      |

## Soporte

Para problemas durante el despliegue, revisar:
1. Logs de Docker (`docker compose logs`)
2. Estado de los contenedores (`docker compose ps`)
3. Conexion de red (`docker network inspect`)
4. Variables de entorno (`docker compose config`)