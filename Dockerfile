FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/project-x-backend-1.0.0-all.jar app.jar

EXPOSE 7070

ENV SERVER_PORT=7070
ENV SERVER_HOST=0.0.0.0

ENTRYPOINT ["java", "-jar", "app.jar"]
