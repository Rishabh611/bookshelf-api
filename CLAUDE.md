# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the app locally
mvn spring-boot:run

# Build JAR (skipping tests)
mvn clean package -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=BookshelfApiApplicationTests

# Build Docker image
docker build -t rishabh611/bookshelf-api:1.0 .

# Run as a container
docker run -p 8080:8080 rishabh611/bookshelf-api:1.0
```

## Architecture

Single-module Spring Boot 3.5.0 / Java 21 REST API with no database — books are stored in an in-memory `List<String>` inside `BookController`.

```
src/main/java/com/bookshelf/api/
├── BookshelfApiApplication.java   # Entry point (@SpringBootApplication)
└── BookController.java            # @RestController, owns the in-memory book list
```

**Endpoints:** `GET /books` — returns all books | `POST /books` — appends a book title (raw string body)

## Infrastructure

**Docker:** Multi-stage Dockerfile — Stage 1 (JDK) builds the JAR, Stage 2 (JRE) runs it, producing a ~180 MB image.

**Kubernetes (`k8s/`):** 2-replica Deployment + NodePort Service (port 30080). Currently targets a local Minikube cluster.

**CI/CD (`Jenkinsfile`):** Linear pipeline — checkout → `mvn clean package` → `docker build` → push to Docker Hub (`rishabh611/bookshelf-api:1.0`) using the `dockerhub-credentials` Jenkins credential → `kubectl apply` both manifests. Jenkins requires Docker group membership and a copied kubeconfig/minikube certs to run Docker and kubectl commands (see `notes.md` for setup steps).

**Next phase:** AWS deployment (not yet started).
