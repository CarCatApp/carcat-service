FROM eclipse-temurin:21-jdk-alpine
# ffmpeg supplies ffprobe for chat voice messages. Java on Alpine skips DNS when nsswitch is missing.
RUN apk add --no-cache ffmpeg \
 && echo "hosts: files dns" > /etc/nsswitch.conf
WORKDIR /app
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY build/libs/carland_service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["/entrypoint.sh"]