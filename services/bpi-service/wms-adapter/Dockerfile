ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG JAVA_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY contracts contracts
COPY services/bpi-service services/bpi-service
RUN mvn -B -ntp -f services/bpi-service/pom.xml \
    -pl wms-adapter -am -DskipTests package

FROM ${JAVA_IMAGE}
RUN groupadd --system --gid 10001 bpi \
    && useradd --system --uid 10001 --gid bpi --home-dir /opt/bpi bpi
WORKDIR /opt/bpi
COPY --from=build --chown=bpi:bpi \
    /workspace/services/bpi-service/wms-adapter/target/bpi-wms-adapter-*-exec.jar \
    /opt/bpi/bpi-wms-adapter.jar
USER 10001:10001
EXPOSE 19092
ENTRYPOINT ["java", "-jar", "/opt/bpi/bpi-wms-adapter.jar"]
