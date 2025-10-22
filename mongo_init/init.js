// Se ejecuta SOLO en el primer arranque, antes de habilitar auth.
// Crea usuario de aplicación en la DB "condos".
db = db.getSiblingDB('condos');
db.createUser({
  user: '***REMOVED***',
  pwd: '***REMOVED***',
  roles: [{ role: 'readWrite', db: 'condos' }]
});
print('>> Created app user auth_user on condos');