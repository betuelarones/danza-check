FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

EXPOSE 8080

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/target/*.jar app.jar

USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]