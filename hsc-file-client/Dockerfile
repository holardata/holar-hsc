# 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
# Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
# -file-client: 文件同步客户端（部署在 FreeSWITCH 服务器上）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/fileClient-0.0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
