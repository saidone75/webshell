# syntax=docker/dockerfile:1.2

ARG MAVEN_VERSION=3-sapmachine-17
ARG JAVA_VENDOR=azul/zulu-openjdk-alpine
ARG JAVA_VERSION=17-jre-headless-latest

# --- Stage 1: Builder ---
FROM maven:${MAVEN_VERSION} as builder

WORKDIR /application

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=bind,source=.m2/settings.xml,target=.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    mvn -s .m2/settings.xml -Dmaven.repo.local=/root/.m2/repository \
    dependency:go-offline -B

COPY ./.m2/settings.xml ./.m2/settings.xml
COPY ./src ./src
COPY ./pom.xml ./pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -s .m2/settings.xml -Dmaven.repo.local=/root/.m2/repository \
    clean package -DskipTests -Dlicense.skip=true

# --- Stage 2: Runtime ---
FROM ${JAVA_VENDOR}:${JAVA_VERSION}
ARG UID=1010
ARG GID=1010

RUN addgroup --gid ${GID} -S spring && adduser --uid ${UID} -S spring -G spring
RUN mkdir /data && chown -R spring:spring /data
USER spring:spring

ARG JAR_FILE=/application/target/webshell-*.war
COPY --from=builder ${JAR_FILE} app.war

ENTRYPOINT ["sh", "-c", "java $JAVA_TOOL_OPTIONS -jar /app.war"]
