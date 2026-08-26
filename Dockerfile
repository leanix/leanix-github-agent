FROM --platform=linux/x86_64 eclipse-temurin:21.0.12_8-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699

RUN apk --no-cache upgrade && apk --no-cache add curl ca-certificates

USER 65534
EXPOSE 8080

COPY build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
