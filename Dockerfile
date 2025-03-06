# Сборка
FROM maven:3.8.6-amazoncorretto-17 AS build
COPY . /app
WORKDIR /app
RUN mvn dependency:go-offline && mvn clean package -DskipTests

FROM amazoncorretto:17
COPY --from=build /app/target/wallet-manager-0.0.1-SNAPSHOT.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]