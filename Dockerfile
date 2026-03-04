# =============================================================================
# Stage 1: Builder — JDK + Node.js + Clojure CLI
# =============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

# Install Node.js 22 LTS
RUN apt-get update && apt-get install -y curl bash rlwrap libatomic1 && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Install Clojure CLI
RUN curl -fsSL https://download.clojure.org/install/linux-install-1.12.0.1530.sh | bash

WORKDIR /build

# -- Dependency caching layer --
# Copy dependency descriptors first so npm/clj deps are cached across builds
COPY package.json package-lock.json ./
# Increment CACHE_BUST when system-level deps change (e.g. new apt packages)
ARG CACHE_BUST=2
RUN npm ci

COPY deps.edn ./
# Pre-download Clojure deps (including :build alias)
RUN clj -P && clj -P -T:build

# -- Source layer --
COPY . .

# Build: postcss → shadow-cljs release → AOT + uberjar
RUN clj -T:build uber

# =============================================================================
# Stage 2: Runtime — JRE only
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the uberjar from builder
COPY --from=builder /build/target/*-standalone.jar /app/app.jar

# Railway injects PORT; default to 8080
ENV PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${PORT}/health || exit 1

ENTRYPOINT ["java"]
CMD ["-jar", "/app/app.jar"]
