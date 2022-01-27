FROM eclipse-temurin:11.0.13_8-jre-alpine

MAINTAINER David Ouagne <david.ouagne@phast.fr>

ENV TZ=Europe/Paris
ENV HOSTNAME=cql-cds-hooks

RUN apk --no-cache add curl jq
ADD build/libs/cds-hooks-0.0.1-SNAPSHOT.jar /home/phast/target/

EXPOSE 9200

#run the spring boot application
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dserver.port=9200", \
"-Deureka.instance.hostname=${HOSTNAME}","-jar","/home/phast/target/cds-hooks-0.0.1-SNAPSHOT.jar"]

HEALTHCHECK --start-period=15s --interval=20s --timeout=10s --retries=5 \
            CMD curl --silent --fail --request GET http://${HOSTNAME}:9200/actuator/health \
                | jq --exit-status '.status == "UP"' || exit 1
