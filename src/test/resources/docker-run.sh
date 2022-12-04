#!/bin/bash

if [ "x$1" = "x" ]; then
  image=query-engine-0-0:`gawk '{ if (match($0, /MAVEN_PROJECT_VERSION = "(.*)"/, a)) print a[1] }'  ../../main/java/uk/co/spudsoft/query/main/Main.java`
else
  image=$1
fi

docker run -it \
  -e query-engine.httpServerOptions.port=8080 \
  -e query-engine.logging.level.uk=TRACE \
  -e query-engine.audit.dataSource.url=jdbc:mysql://host.docker.internal:2001/query-engine?useSSL=false\&serverTimezone=UTC \
  -e query-engine.audit.dataSource.user.username=auditor \
  -e query-engine.audit.dataSource.user.password=TopSecret \
  -e query-engine.audit.dataSource.adminUser.username=auditor-ddl \
  -e query-engine.audit.dataSource.adminUser.password=TopSecret \
  -e query-engine.zipkin.baseUrl=http://localhost/wontwork \
  -e query-engine.designMode=true \
  -p 8080:8080 \
  -v /mnt/c/Work/github/query-engine/src/test/resources/sources:/var/query-engine \
  $image