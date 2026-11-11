FROM --platform=linux/x86_64 eclipse-temurin:21.0.13_9-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

RUN apk --no-cache upgrade && apk --no-cache add curl ca-certificates

USER 65534
EXPOSE 8080

COPY build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
