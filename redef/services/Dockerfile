FROM java:8

RUN apt-get update && apt-get install -y \
    mysql-client \
    && rm -rf /var/lib/apt/lists/*

ADD build/libs/services-1.0-SNAPSHOT-standalone.jar /services-1.0-SNAPSHOT-standalone.jar

ADD entrypoint.sh /entrypoint.sh

CMD /entrypoint.sh \
    -DKOHA_API_USER="$KOHA_API_USER" \
    -DKOHA_API_PASS="$KOHA_API_PASS" \
    -DDATA_BASEURI="$DATA_BASEURI" \
    -Done-jar.silent=true \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5070 \
    -jar /services-1.0-SNAPSHOT-standalone.jar
