FROM openjdk:11-jdk-slim
WORKDIR /app
COPY target/userms-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
