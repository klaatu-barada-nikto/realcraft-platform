# 阶段 1：构建（Maven + JDK 21）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B -DskipTests package

# 阶段 2：运行时（JRE 21 Alpine）
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata
COPY --from=build /workspace/target/app.jar /app/app.jar
COPY dist /app/dist
RUN mkdir -p /app/data/images /app/data/models
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
