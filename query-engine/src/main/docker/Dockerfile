FROM gcr.io/distroless/java25-debian13:nonroot

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
