FROM --platform=linux/x86_64 eclipse-temurin:21.0.8_9-jre-alpine@sha256:990397e0495ac088ab6ee3d949a2e97b715a134d8b96c561c5d130b3786a489d

RUN apk --no-cache upgrade && apk --no-cache add curl ca-certificates

USER 65534
EXPOSE 8080

COPY build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
