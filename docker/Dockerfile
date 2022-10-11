ARG KEYCLOAK_IMAGE

FROM $KEYCLOAK_IMAGE

USER root
COPY plugins/*.jar /opt/keycloak/providers/
USER 1000

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
