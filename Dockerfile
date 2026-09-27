FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]