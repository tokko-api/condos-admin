// Se ejecuta SOLO en el primer arranque, antes de habilitar auth.
// Crea usuario de aplicación en la DB "condos".
// Las credenciales se leen de variables de entorno (definidas en .env / docker-compose)
// para no dejar secretos hardcodeados en el repo.
var appUser = process.env.APP_MONGO_USER;
var appPwd = process.env.APP_MONGO_PASSWORD;

if (!appUser || !appPwd) {
  throw new Error('APP_MONGO_USER y APP_MONGO_PASSWORD deben estar definidas al iniciar el contenedor de mongo');
}

db = db.getSiblingDB('condos');
db.createUser({
  user: appUser,
  pwd: appPwd,
  roles: [{ role: 'readWrite', db: 'condos' }]
});
print('>> Created app user ' + appUser + ' on condos');