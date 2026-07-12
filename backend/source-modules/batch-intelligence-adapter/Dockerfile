ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-8
ARG JAVA_IMAGE=eclipse-temurin:8-jre-jammy

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
COPY backend/source-modules/pom.xml backend/source-modules/pom.xml
COPY backend/source-modules/batch-intelligence-adapter backend/source-modules/batch-intelligence-adapter
RUN mvn -q -f backend/source-modules/pom.xml -pl batch-intelligence-adapter -am -DskipTests package

FROM ${JAVA_IMAGE}
WORKDIR /opt/ft-mes
RUN groupadd --system ftmes && useradd --system --gid ftmes --home-dir /opt/ft-mes ftmes
COPY --from=build /workspace/backend/source-modules/batch-intelligence-adapter/target/batch-intelligence-adapter-*.jar app.jar
USER ftmes
EXPOSE 19080
ENTRYPOINT ["java", "-jar", "/opt/ft-mes/app.jar"]
