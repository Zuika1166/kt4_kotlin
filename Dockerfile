FROM gradle:9.7.1-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/build/install/kt4-kotlin/ .
EXPOSE 8080
CMD ["bin/kt4-kotlin"]
