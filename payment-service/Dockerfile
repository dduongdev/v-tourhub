FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -N install

COPY soa-common/pom.xml soa-common/pom.xml
COPY soa-common/src soa-common/src
RUN mvn -B -f soa-common/pom.xml install -DskipTests

COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src
RUN mvn -B -f payment-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/payment-service/target/*.jar app.jar

RUN apk --no-cache add curl
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]
