FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Import corporate SSL interception CA certificates into Java truststore
COPY corporate-ca.crt /tmp/corporate-ca.crt
COPY import-certs.sh /tmp/import-certs.sh
RUN sed -i 's/\r$//' /tmp/import-certs.sh /tmp/corporate-ca.crt && chmod +x /tmp/import-certs.sh && /tmp/import-certs.sh

# Copy all module POMs for reactor resolution
COPY pom.xml .
COPY eretain-common/pom.xml eretain-common/pom.xml
COPY eretain-discovery-server/pom.xml eretain-discovery-server/pom.xml
COPY eretain-api-gateway/pom.xml eretain-api-gateway/pom.xml
COPY eretain-auth-service/pom.xml eretain-auth-service/pom.xml
COPY eretain-company-service/pom.xml eretain-company-service/pom.xml
COPY eretain-project-service/pom.xml eretain-project-service/pom.xml
COPY eretain-allocation-service/pom.xml eretain-allocation-service/pom.xml
COPY eretain-timesheet-service/pom.xml eretain-timesheet-service/pom.xml
COPY eretain-reporting-service/pom.xml eretain-reporting-service/pom.xml
COPY eretain-ui/pom.xml eretain-ui/pom.xml

# Download dependencies
RUN mvn dependency:go-offline -pl eretain-discovery-server -am -B 2>/dev/null || true

# Copy source and build
COPY eretain-common/src eretain-common/src
COPY eretain-discovery-server/src eretain-discovery-server/src
RUN mvn clean package -pl eretain-discovery-server -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eretain-discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
