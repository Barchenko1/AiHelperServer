# syntax=docker/dockerfile:1.7

########## Build ##########
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /src
RUN useradd -u 1001 nonroot
RUN --mount=type=cache,id=gradle-cache,target=/home/gradle/.gradle true
COPY --chown=gradle:gradle . .
RUN --mount=type=cache,id=gradle-cache,target=/home/gradle/.gradle \
    gradle clean bootJar -x test --no-daemon

########## Run (Temurin JRE) ##########
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
USER 1001:1001
COPY --from=build /src/build/libs/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENV OPEN_AI_API_KEY=""
ENV APP_JWT_SECRET=""
ENV APP_NOOP_SECRET=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
