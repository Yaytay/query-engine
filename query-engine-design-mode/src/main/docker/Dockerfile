# ==========================================
# STAGE 1: Build & Custom Runtime Generation
# ==========================================
FROM azul-zulu:25.38-25.0.5.1-jre-headless-debian13 AS builder

# Set environment variables for the runtime
ENV JAVA_HOME=/opt/jdk
ENV PATH="$JAVA_HOME/bin:$PATH"

ARG DEPS
ARG mainClass

# Set working directory
WORKDIR /query-engine

# Copy application bits from the builder stage
COPY target/dependencies /query-engine/dependencies/
COPY target/*.jar /query-engine/

# Configure classpath and entrypoint environment variables
ENV CLASSPATH=${DEPS}

EXPOSE 8080



# Distroless nonroot user runs with UID 65532 by default
ENTRYPOINT ["java", \
  "-XX:-OmitStackTraceInFastThrow", \
  "${mainClass}"]

HEALTHCHECK CMD "java", \
              "-XX:-OmitStackTraceInFastThrow", \
              "${mainClass}" \
              "--healthCheck"
