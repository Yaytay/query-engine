FROM gcr.io/distroless/java25-debian13:nonroot

ARG DEPS
ARG mainClass

# Set working directory
WORKDIR /app

# Copy application bits from the builder stage
COPY target/dependencies /dependencies
COPY target/*.jar /

# Configure classpath and entrypoint environment variables
ENV CLASSPATH=${DEPS}:/app.jar

EXPOSE 8080

# Distroless nonroot user runs with UID 65532 by default
ENTRYPOINT ["java", \
  "-XX:-OmitStackTraceInFastThrow", \
  "${mainClass}"]
