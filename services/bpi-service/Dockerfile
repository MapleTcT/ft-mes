ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG JAVA_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY services/bpi-service services/bpi-service
RUN mvn -q -f services/bpi-service/pom.xml -DskipTests package

FROM ${JAVA_IMAGE}
RUN groupadd --system --gid 10001 bpi \
    && useradd --system --uid 10001 --gid bpi --home-dir /opt/bpi bpi
WORKDIR /opt/bpi
COPY --from=build --chown=bpi:bpi \
    /workspace/services/bpi-service/app/target/bpi-service-*.jar /opt/bpi/bpi-service.jar
USER 10001:10001
EXPOSE 19091
ENTRYPOINT ["java", "-jar", "/opt/bpi/bpi-service.jar"]
