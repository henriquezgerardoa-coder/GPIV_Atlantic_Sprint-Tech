# ETAPA 1: Compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copiamos el POM raíz
COPY pom.xml .

# 2. Copiamos TODOS los módulos definidos en el POM raíz
# Esto evita el error "Child module ... does not exist"
COPY backend ./backend
COPY entities ./entities
COPY commons ./commons
COPY frontend ./frontend

# 3. Compilamos solo el backend (y sus dependencias automáticas)
RUN mvn clean package -pl backend -am -DskipTests

# ETAPA 2: Ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app

# Ajustar la zona horaria local
ENV TZ=America/Argentina/Buenos_Aires

# Crear un usuario y grupo sin privilegios
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8090"]