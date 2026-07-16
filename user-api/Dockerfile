# ===== build stage: compila el modulo con Maven (contexto = raiz del repo) =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 1) POMs primero: el pom agregador referencia TODOS los modulos, asi que
#    deben existir para construir el reactor, aunque solo compilemos este.
#    Copiar solo los POMs permite cachear la descarga de dependencias.
COPY pom.xml ./
COPY shared-lib/pom.xml shared-lib/
COPY auth-api/pom.xml auth-api/
COPY tenant-api/pom.xml tenant-api/
COPY user-api/pom.xml user-api/
COPY board-api/pom.xml board-api/
RUN mvn -pl user-api -am -DskipTests dependency:go-offline || true

# 2) Fuentes necesarias (este modulo + shared-lib) y build del jar
COPY shared-lib/src shared-lib/src
COPY user-api/src user-api/src
RUN mvn -pl user-api -am -DskipTests clean package

# ===== runtime stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/user-api/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
