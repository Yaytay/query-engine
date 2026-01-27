# Full JDK image with all JDK/bin tools available, plus optional AppCDS warmup

# Build + warmup stage (keeps full JDK)
FROM azul/zulu-openjdk-alpine:25.0.2-jdk AS build

ARG DEPS
ARG mainClass

ENV JAVA_HOME=/usr/lib/jvm/zulu25
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV CLASSPATH=${DEPS}

# App bits (dependencies + runnable artifact[s])
# - Expect dependencies prepared under target/dependencies and fat/thin jar(s) under target/
COPY target/dependencies /dependencies
COPY target/*.jar /

# Optional: generate AppCDS archive (works only on the same JVM build)
# Note: AppCDS on Alpine musl is supported in modern Zulu builds; if not needed, comment this out
RUN mkdir -p /cds && \
    java \
      -XX:ArchiveClassesAtExit=/cds/shared.jsa \
      -XX:-OmitStackTraceInFastThrow \
      ${mainClass} \
      '--jwt.acceptableIssuerRegexes[0]=.*' \
      --ExitOnRun || true

# Final runtime stage: keep the WHOLE JDK to retain all executables (javac, jcmd, jmap, jstat, etc.)
FROM azul/zulu-openjdk-alpine:25.0.2-jdk

ARG DEPS
ARG mainClass

ENV JAVA_HOME=/usr/lib/jvm/zulu25
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV CLASSPATH=${DEPS}

# Copy AppCDS (if created) and app bits
COPY --from=build /cds /cds
COPY --from=build /dependencies /dependencies
COPY --from=build /*.jar /

EXPOSE 8080

# Use AppCDS if present; JVM ignores missing/empty archive gracefully in recent builds,
# but to be safe, pass both shared archive options; JVM uses the last one if both exist.
ENTRYPOINT ["java", \
  "-XX:SharedArchiveFile=/cds/shared.jsa", \
  "-XX:-OmitStackTraceInFastThrow", \
  "${mainClass}"]
