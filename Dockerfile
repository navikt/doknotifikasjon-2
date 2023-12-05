FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics
ENV APPD_ENABLED=true

COPY app/target/app.jar /app/app.jar
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
USER root
RUN apt-get install -y --no-install-recommends jq
RUN chmod +x /run-java.sh
USER apprunner

ENV JAVA_OPTS="-Xmx1024m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"