# Build stage — compiles the Ktor app into a runnable "installation" (app dir + all
# dependency jars + launch script) via the Gradle `application` plugin's distribution task.
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew installDist --no-daemon --console=plain

# Run stage — slim JRE image, only the built distribution is copied in, so the JDK/Gradle
# cache/source tree from the build stage never end up in the final image.
FROM eclipse-temurin:26-jre AS run
WORKDIR /app

COPY --from=build /app/build/install/neet-backend ./

EXPOSE 8080
ENTRYPOINT ["./bin/neet-backend"]
