FROM azul/zulu-openjdk-alpine:19.0.2

RUN jlink --output /qe-java --add-modules java.base,java.management,jdk.management,jdk.unsupported,java.sql.rowset,java.desktop,jdk.naming.dns

FROM alpine:3.17.3

COPY --from=0 /qe-java /qe-java
RUN ln -s /qe-java/bin/java /usr/bin/java

ENV JAVA_HOME /qe-java/bin

LABEL version=${project.version} \
      major-version=${parsedVersion.majorVersion} \
      minor-version=${parsedVersion.majorVersion}.${parsedVersion.minorVersion} \
      artifactId=${project.artifactId}

EXPOSE 8080

COPY target/dependencies /${project.artifactId}
COPY target/${project.artifactId}-${project.version}.jar /${project.artifactId}

ENTRYPOINT exec java -cp "/${project.artifactId}/*" ${mainClass}

