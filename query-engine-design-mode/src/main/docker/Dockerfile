# ---- Stage 0: Build a jlinked runtime and create a base CDS archive ----
FROM azul/zulu-openjdk-alpine:25.0.1-jdk AS jlink

# Create minimized Java runtime
RUN jlink \
    --output /qe-java \
    --add-modules java.base,java.compiler,java.management,java.net.http,jdk.management,jdk.unsupported,java.sql.rowset,java.desktop,jdk.naming.dns,java.naming,java.security.jgss,java.security.sasl,java.smartcardio,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.security.jgss \
    --compress=2 \
    --no-header-files \
    --no-man-pages

# Generate base class list and base CDS archive for the jlinked runtime
# - Place it where HotSpot expects it for the server VM layout
RUN /qe-java/bin/java -Xshare:off -XX:DumpLoadedClassList=/tmp/base.classlist -version && \
    /qe-java/bin/java -Xshare:dump \
      -XX:SharedClassListFile=/tmp/base.classlist \
      -XX:SharedArchiveFile=/qe-java/lib/server/classes.jsa

# ---- Stage 1: Produce application CDS archive on top of base CDS ----
FROM azul/zulu-openjdk-alpine:25.0.1-jdk AS appcds

ARG DEPS
ARG mainClass

ENV JAVA_HOME=/qe-java
ENV CLASSPATH=${DEPS}

COPY --from=jlink /qe-java /qe-java

# Application bits (dependencies + runnable artifact[s])
COPY target/dependencies /dependencies
COPY target/*.jar /

# Warm run to generate application CDS archive, building on the base CDS
# - If the app exits non‑zero due to --ExitOnRun semantics, ignore that exit code
RUN mkdir -p /cds && \
    /qe-java/bin/java \
      -XX:SharedArchiveFile=/qe-java/lib/server/classes.jsa \
      -XX:ArchiveClassesAtExit=/cds/shared.jsa \
      -XX:-OmitStackTraceInFastThrow \
      ${mainClass} \
      '--jwt.acceptableIssuerRegexes[0]=.*' \
      --ExitOnRun || rm -Rf /var/query-engine/* 

# ---- Final Stage: tiny image with jlinked runtime + CDS archives ----
FROM alpine:3.22.2

ARG DEPS
ARG mainClass

ENV JAVA_HOME=/qe-java
ENV CLASSPATH=${DEPS}

# Runtime + CDS
COPY --from=appcds /qe-java /qe-java
COPY --from=appcds /cds /cds

# Application bits
COPY --from=appcds /dependencies /dependencies
COPY --from=appcds /*.jar /

EXPOSE 8080

# Use both base CDS (from jlink stage) and app CDS (from warm run)
ENTRYPOINT ["/qe-java/bin/java", \
  "-XX:SharedArchiveFile=/qe-java/lib/server/classes.jsa", \
  "-XX:SharedArchiveFile=/cds/shared.jsa", \
  "-XX:-OmitStackTraceInFastThrow", \
  "${mainClass}"]
