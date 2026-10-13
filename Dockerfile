FROM --platform=linux/x86_64 eclipse-temurin:21.0.12_8-jre-alpine@sha256:1a29e1fe337eb28b5bec30f0ee8ed29f0ff80ab6f75dcf9313efe82911065a52

RUN apk --no-cache upgrade && apk --no-cache add curl ca-certificates

USER 65534
EXPOSE 8080

COPY build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
