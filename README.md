[日本語で読む (Japanese)](README.ja.md)

# Simple File Storage Server & CLI

[![Clean Build](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/gradle.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/codeql.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/codeql.yml)
[![Docker Build](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/drag0sd0g/MultipartUploadService/actions/workflows/docker-publish.yml)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Quarkus Version](https://img.shields.io/badge/Quarkus-3.17.4-blue.svg)](https://quarkus.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, containerized file storage service with REST API, built with Quarkus and Java 21.

## 📋 Table of Contents

1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Quick Start with Docker](#quick-start-with-docker)
4. [Build & Packaging Instructions](#build--packaging-instructions)
5. [Running the Server](#running-the-server)
6. [Running the Client](#running-the-client)
7. [Observability](#observability)
8. [Development](#development)
9. [Testing](#testing)

---

## ✨ Features

- 🚀 **Modern Stack**: Built with Quarkus 3.17.4 and Java 21
- 🐳 **Containerized**: Docker images for both server and client
- 📊 **Observability**: Prometheus metrics and structured logging
- 🔒 **Security**: CodeQL scanning and dependency updates via Dependabot
- ✅ **Quality**: Checkstyle, PMD, and SpotBugs static analysis
- 🧪 **Tested**: 80% code coverage + comprehensive integration tests
- 📖 **API Documentation**: OpenAPI/Swagger UI included
- 🌐 **REST API**: Simple, well-documented file storage API

---

## Prerequisites

### For Running with Docker (Recommended)
- Docker 20.10+
- Docker Compose 2.0+

### For Building from Source
- JDK 21 or newer (download from [Adoptium](https://adoptium.net/))
- JAVA_HOME environment variable set to the JDK installation directory

---

## 🚀 Quick Start with Docker

The fastest way to get started is using Docker Compose:

```bash
# Clone the repository
git clone https://github.com/drag0sd0g/MultipartUploadService.git
cd MultipartUploadService

# Start both server and client
docker-compose up -d

# Check server health
curl http://localhost:8080/q/health

# View logs
docker-compose logs -f server

# Stop services
docker-compose down
```

The server will be available at http://localhost:8080 with:
- REST API: http://localhost:8080/v1/files
- Swagger UI: http://localhost:8080/q/swagger-ui/
- Prometheus Metrics: http://localhost:8080/q/metrics
- Health Check: http://localhost:8080/q/health

---

## Build & Packaging Instructions

Run one of the following commands from the repository root directory to build the binaries of both client and server.

### On Windows

```bat
gradlew clean build distClient distServer
```

### On Unix

```bash
chmod +x gradlew && ./gradlew clean build distClient distServer
```

Client and server jars will be deployed in the _build_ folder under _fsclient/_ and _fsserver/_ directories.

### Building Docker Images

```bash
# Build server image
docker build -f file-storage-server/Dockerfile -t file-storage-server:latest .

# Build client image
docker build -f file-storage-client/Dockerfile -t file-storage-client:latest .
```

---

## Running the Server

### Using Docker (Recommended)

```bash
docker run -p 8080:8080 -v $(pwd)/data-server:/app/data-server file-storage-server:latest
```

### Using Java

To run the file storage server navigate to _build/fsserver_ and then run:

```shell
java -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

Properties are kept in the _application.properties_ file under _src/main/resources_ but there are also plenty
of default properties assumed by Quarkus. If you wish to **override** some of these (for example the **port** number or **host**), you can do
so by passing the override with _-D_ args to the jar e.g.

```shell
java -Dquarkus.http.host="192.168.11.7" -Dquarkus.http.port=8085 -jar file-storage-server-1.0.0-SNAPSHOT.jar
```

### Server Notes

- By default, the server will start up at http://127.0.0.1:8080
- REST API documentation available at http://127.0.0.1:8080/q/swagger-ui/
- OpenAPI spec available at http://127.0.0.1:8080/q/openapi
- Quarkus framework stores _multipart/form-data_ files in a temporary location from where they will be copied to
  a designated persistent storage folder (called _data-server_). Quarkus automatically removes the files from the
  temporary location after serving the request
- While no total storage limit is imposed on the server side, a limit of 10MB is set on each file we want to upload.
  This is driven by the config _quarkus.http.limits.max-form-attribute-size_ and any file exceeding that size will yield
  a HTTP 413 (CLI can also fetch this limit via a **GET** to _/v1/stats/fileUploadSizeLimit_)
- This initial REST API version is **/v1**

---

## Running the Client

### Using Docker

```bash
# List files
docker run --network host file-storage-client:latest -l

# Upload a file
docker run --network host -v $(pwd):/data file-storage-client:latest -u /data/myfile.txt

# Delete a file
docker run --network host file-storage-client:latest -d myfile.txt
```

### Using Java

From a separate command line, navigate to _build/fsclient_ and then run one of the three possible commands:

### Listing all uploaded files

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --list-files
```

or

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### Uploading a file

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --upload-file <relative_or_absolute_path_to_file>
```

or

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -u <relative_or_absolute_path_to_file>
```

### Deleting an uploaded file

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar --delete-file <file_name>
```

or

```shell
java -jar file-storage-client-1.0.0-SNAPSHOT.jar -d <file_name>
```

For deletion, only file name is sufficient, no need to provide a path.

By default, the client will attempt to find the server at http://127.0.0.1:8080. You can however choose to **overwrite**
this value by passing the system property _-Dfsserver.api.rootUrl_ to the CLI executable e.g.

```shell
java -Dfsserver.api.rootUrl="http://192.168.11.7:8085" -jar file-storage-client-1.0.0-SNAPSHOT.jar -l
```

### Client Notes

- The client will expect certain command line options and/or arguments. If they are not provided, or provided wrongly
  the CLI will exit and a usage guide will be printed

---

## 📊 Observability

### Prometheus Metrics

The server exposes Prometheus-compatible metrics at `/q/metrics`:

```bash
curl http://localhost:8080/q/metrics
```

Metrics include:
- JVM metrics (memory, GC, threads)
- HTTP server metrics (requests, response times)
- System metrics (CPU, file descriptors)

### Health Checks

```bash
# Overall health
curl http://localhost:8080/q/health

# Liveness probe
curl http://localhost:8080/q/health/live

# Readiness probe
curl http://localhost:8080/q/health/ready
```

### Structured Logging

The server supports structured logging with configurable formats. See `application.properties` for logging configuration.

---

## 🛠 Development

### Code Quality Tools

This project uses multiple static analysis tools:

- **Checkstyle**: Code style enforcement
- **PMD**: Code quality analysis
- **SpotBugs**: Bug pattern detection

Run static analysis:

```bash
./gradlew checkstyleMain pmdMain spotbugsMain
```

### Integration Tests

Comprehensive integration tests using Testcontainers:

```bash
./gradlew :integration-tests:test
```

The integration tests cover:
- ✅ Happy paths (upload, list, delete)
- ❌ Error scenarios (duplicate files, oversized files, missing files)
- 📊 Metrics and health endpoints
- 📖 API documentation endpoints

### Local Development with Docker Compose

```bash
# Start all services in development mode
docker-compose up

# Rebuild and restart after code changes
docker-compose up --build

# View logs
docker-compose logs -f

# Run integration tests against the environment
./gradlew :integration-tests:test
```

---

## Testing

- **Unit Tests**: JUnit 5 with Mockito
- **Integration Tests**: Testcontainers + Docker Compose
- **Code Coverage**: Jacoco reporting with 80% threshold
  - Test reports visible in _build/jacocoHtml/index.html_
- **Continuous Integration**: GitHub Actions with automated testing

Run all tests:

```bash
./gradlew clean test
```

Run with coverage:

```bash
./gradlew clean build
# View coverage report: open file-storage-server/build/jacocoHtml/index.html
```

---

## 📚 API Documentation

After starting the server, interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/q/swagger-ui/
- **OpenAPI Spec**: http://localhost:8080/q/openapi

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

---

## 🔄 Continuous Integration

- **Build**: Automated builds on every push
- **Tests**: Comprehensive test suite with coverage reporting
- **Security**: CodeQL scanning for security vulnerabilities
- **Dependencies**: Automated dependency updates via Dependabot
- **Docker**: Automated Docker image builds and publishing
- **Releases**: Automated release creation and artifact publishing

---

**Built with ❤️ using Quarkus and Java 21**
