FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system blog \
    && useradd --system --gid blog --home-dir /app --shell /usr/sbin/nologin blog

COPY --from=build --chown=blog:blog /workspace/target/personal-blog-backend-*.jar app.jar

USER blog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
