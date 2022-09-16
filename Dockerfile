FROM eclipse-temurin:11.0.16_8-jre-alpine

MAINTAINER David Ouagne <david.ouagne@phast.fr>

ENV TZ=Europe/Paris
ENV HOSTNAME=cql-cds-hooks
ENV DISCOVERY_ENABLED=false
ENV DISCOVERY_URL=http://discovery.edge-service:9102/eureka/
ENV ZIPKIN_ENABLED=false
ENV ZIPKIN_URL=http://zipkin:9411
ENV CIO_URL=https://recette.phast.fr/resources-server_Atelier/api/FHIR
ENV CIO_CREDENTIAL="TODO: ASK PHAST"
ENV TIO_URL=https://recette.phast.fr/resources-server_Atelier/api/FHIR
ENV TIO_CREDENTIAL="TODO: ASK PHAST"

RUN apk --no-cache add curl jq
ADD build/libs/cql-cds-hooks-0.0.1-SNAPSHOT.jar /home/phast/target/

EXPOSE 9200

#run the spring boot application
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dserver.port=9200", \
"-Deureka.client.enabled=${DISCOVERY_ENABLED}","-Deureka.instance.hostname=${HOSTNAME}", \
"-Deureka.client.service-url.defaultZone=${DISCOVERY_URL}","-Dspring.zipkin.enabled=${ZIPKIN_ENABLED}", \
"-Dspring.zipkin.base-url=${ZIPKIN_URL}","-Dcio.cds.server.uri=${CIO_URL}","-Dcio.cds.server.credential=${CIO_CREDENTIAL}", \
"-Dtio.server.uri=${TIO_URL}","-Dtio.server.credential=${TIO_CREDENTIAL}","-jar","/home/phast/target/cql-cds-hooks-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --start-period=15s --interval=20s --timeout=10s --retries=5 \
            CMD curl --silent --fail --request GET http://${HOSTNAME}:9200/actuator/health \
                | jq --exit-status '.status == "UP"' || exit 1
