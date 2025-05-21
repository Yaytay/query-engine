FROM azul/zulu-openjdk-alpine:21.0.7-jdk

RUN jlink --output /qe-java --add-modules java.base,java.compiler,java.management,java.net.http,jdk.management,jdk.unsupported,java.sql.rowset,java.desktop,jdk.naming.dns,java.naming,java.security.jgss,java.security.sasl,java.smartcardio,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.security.jgss 

FROM alpine:3.21.3

ARG DEPS

COPY --from=0 /qe-java /qe-java

LABEL version=${project.version} \
      major-version=${parsedVersion.majorVersion} \
      minor-version=${parsedVersion.majorVersion}.${parsedVersion.minorVersion} \
      artifactId=${project.artifactId}

EXPOSE 8080

COPY target/dependencies /dependencies
COPY target/${project.artifactId}-${project.version}* /

ENV JAVA_HOME=/qe-java
ENV CLASSPATH=${DEPS}

# Create the shared archive using the calculated classpath. This will only 
# contain application classes because it builds on top of the default Java-class
# archive that the JDK contains since version 12.
#
# See https://blog.codefx.org/java/application-class-data-sharing/#Working-With-A-JDK-Class-Data-Archive
RUN mkdir /cds && /qe-java/bin/java -XX:ArchiveClassesAtExit=/cds/shared.jsa -XX:-OmitStackTraceInFastThrow ${mainClass} '--jwt.acceptableIssuerRegexes[0]=.*' --ExitOnRun || rm -Rf /var/query-engine/* 

ENTRYPOINT ["/qe-java/bin/java", "-XX:SharedArchiveFile=/cds/shared.jsa", "-XX:-OmitStackTraceInFastThrow", "${mainClass}"]
