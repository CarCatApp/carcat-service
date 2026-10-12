FROM eclipse-temurin:21-jdk-alpine
# Java on Alpine skips DNS when this file is missing.
RUN echo "hosts: files dns" > /etc/nsswitch.conf
WORKDIR /app
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY build/libs/carland_service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["/entrypoint.sh"]