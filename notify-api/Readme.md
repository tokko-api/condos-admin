# 🛡️ Auth API – Condos Platform

Servicio responsable de **autenticación** y **emisión de tokens JWT** para la plataforma Condos.  
Valida credenciales contra `AuthAccountRepository`, obtiene asignaciones desde `user-api` y genera tokens firmados con roles activos.

---

## 📍 Base Path
/condos/api/auth
---

## 🚀 Endpoints principales

| Método | Endpoint | Descripción |
|:--------|:----------|:-------------|
| `POST` | `/login` | Autentica usuario, valida contraseña, obtiene roles activos y genera un JWT. |
| `GET` | `/me` | Retorna el perfil actual extraído del token JWT. |

---

### 🔑 Ejemplo de autenticación

#### Request
```bash
curl -X POST http://localhost:8080/condos/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
        "email": "admin@condos.com",
        "password": "Secret123",
        "orgId": "68d340950811675bf30352fc"
      }'
```   
Response
```bash
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "orgId": "68d340950811675bf30352fc",
  "roles": ["ADMINISTRADOR"],
  "user": { "id": "68d...22969", "email": "admin@condos.com" }
}
```  

🧩 Lógica general
•	Solo las organizaciones con status = ACTIVE se incluyen en el JWT.
•	Si el usuario es SUPERADMIN, siempre obtiene roles = ["SUPERADMIN"].
•	orgId se resuelve automáticamente:
•	Si se envía → se usa.
•	Si no se envía:
•	SUPERADMIN → primera org activa o raíz "000000000000000000000000".
•	Usuario normal → si tiene una única org activa.
•	Si hay varias y no se especifica → 403 Forbidden.

Test rápido de /me

```bash
curl -X GET http://localhost:8080/condos/api/auth/me \
  -H "Authorization: Bearer <token>"
```  

```bash
{
"id": "68d...22969",
"email": "admin@condos.com",
"orgs": [
{ "orgId": "68d340950811675bf30352fc", "role": "ADMINISTRADOR" },
{ "orgId": "abc123", "role": "SUPERVISOR" }
],
"ver": 1
}
```  
📘 Documentación OpenAPI (Swagger)

La especificación completa se encuentra en openapi.yaml


