# ═══════════════════════════════════════════════════════════════════
# IKOS — Identity Knowledge Operating System
# Multi-stage Docker build: Build (JDK) → Run (JRE) → Serve
# ═══════════════════════════════════════════════════════════════════

# Stage 1: Build with Maven (JDK needed for javac compilation)
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy entire project (respects .dockerignore)
COPY . .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the full project with dependencies
RUN ./mvnw clean compile -pl ikos -am -q

# Stage 2: Runtime with JRE (lighter than JDK, can run Java but not compile)
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Install Python for dashboard HTTP server
RUN apt-get update && apt-get install -y --no-install-recommends python3 && \
    rm -rf /var/lib/apt/lists/*

# Copy compiled project from builder (includes .class files + Maven cache)
COPY --from=builder /app /app
COPY --from=builder /root/.m2 /root/.m2

# Copy the pre-built dashboard with Identity Graph, Case Studies, etc.
COPY ikos-dashboard.html /app/output/ikos-dashboard.html

# Copy docs alongside dashboard for judge access
COPY docs/ /app/output/docs/
COPY DEMO_SCRIPT.md /app/output/DEMO_SCRIPT.md
COPY README.md /app/output/README.md

# Create index that redirects to dashboard
RUN echo '<html><head><meta http-equiv="refresh" content="0;url=ikos-dashboard.html"></head></html>' > /app/output/index.html

# Create output directory
RUN mkdir -p /app/output

# Expose dashboard port
EXPOSE 8080

# Run full IKOS pipeline → generate dashboard → serve it
# Judges see: live pipeline output in terminal + dashboard at http://localhost:8080
CMD ["sh", "-c", "\
    echo '' && \
    echo '⛨═══════════════════════════════════════════════════════════════' && \
    echo '⛨  IKOS — Identity Knowledge Operating System' && \
    echo '⛨  Full Pipeline Execution + Dashboard Server' && \
    echo '⛨═══════════════════════════════════════════════════════════════' && \
    echo '' && \
    echo '▶ Running QuickDashboard pipeline (SimulatedData → Correlate → Risk → Behavioral → Dashboard)...' && \
    echo '' && \
    ./mvnw exec:java -pl ikos \
        -Dexec.mainClass=org.springaicommunity.agent.ikos.QuickDashboard \
        -Dikos.storage=/app/output -q 2>/dev/null && \
    echo '' && \
    echo '✅ Pipeline complete. Dashboard ready.' && \
    echo '🌐 Open: http://localhost:8080' && \
    echo '' && \
    echo '───────────────────────────────────────────────────────────────' && \
    echo '  To run the interactive CLI demo:' && \
    echo '  docker exec -it ikos-dashboard ./mvnw exec:java -pl ikos \\' && \
    echo '    -Dexec.mainClass=org.springaicommunity.agent.ikos.IkosDemo' && \
    echo '───────────────────────────────────────────────────────────────' && \
    echo '' && \
    cd /app/output && python3 -m http.server 8080 \
"]
