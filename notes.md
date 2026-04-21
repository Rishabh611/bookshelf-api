# BookShelf API — DevOps Learning Journey

A living reference doc tracking the full journey of building and deploying a Spring Boot REST API using Docker, Kubernetes, Maven, CI/CD, and AWS.

---

## The Project

**App:** BookShelf API — a simple Spring Boot REST API to manage a list of books.  
**Goal:** Take this app through the full DevOps lifecycle as a hands-on learning project.

### Pipeline we're building
```
Code → Maven Build → Docker Image → Kubernetes Cluster → CI/CD Pipeline → AWS
```

---

## Environment Setup

| Tool       | Version         |
|------------|-----------------|
| OS         | WSL Ubuntu (Windows) |
| Java       | OpenJDK 25.0.2  |
| Maven      | 3.8.7           |
| Docker     | 28.1.1          |

---

## Phase 1 — Spring Boot App + Maven

### What we did
- Bootstrapped a Spring Boot 3.5.0 project using Spring Initializr (via `curl`)
- Added a `BookController` with two endpoints
- Ran the app locally with Maven

### Key commands
```bash
# Generate project
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.5.0 \
  -d baseDir=bookshelf-api \
  -d groupId=com.bookshelf \
  -d artifactId=bookshelf-api \
  -d name=bookshelf-api \
  -d packageName=com.bookshelf.api \
  -d dependencies=web \
  -d javaVersion=21 \
  -o bookshelf-api.zip

unzip bookshelf-api.zip && cd bookshelf-api

# Run app
mvn spring-boot:run
```

### API endpoints
| Method | Endpoint  | Description       |
|--------|-----------|-------------------|
| GET    | /books    | Get all books     |
| POST   | /books    | Add a new book    |

### BookController.java
```java
package com.bookshelf.api;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final List<String> books = new ArrayList<>(
        List.of("Dune", "Clean Code", "The Pragmatic Programmer")
    );

    @GetMapping
    public List<String> getAllBooks() {
        return books;
    }

    @PostMapping
    public String addBook(@RequestBody String title) {
        books.add(title);
        return "Added: " + title;
    }
}
```

### Test commands
```bash
curl http://localhost:8080/books

curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '"Kafka on the Shore"'
```

### Why Maven?
Maven manages the full build lifecycle — compiling, testing, packaging. The `pom.xml` is the single source of truth for dependencies and build config. Running `mvn spring-boot:run` compiles and starts the app in one step.

---

## Phase 2 — Docker 🐳

### Why Docker?
The app currently only runs on your machine. Docker packages the app + its entire environment (Java runtime, dependencies) into a portable **container image** that runs identically anywhere — locally, on AWS, or in Kubernetes.

> "Don't share code, share the environment itself."

### Steps
- [x] Build the JAR with Maven
- [x] Write a Dockerfile
- [x] Build a Docker image
- [x] Run as a container
- [x] Push to Docker Hub

### Step 1 — Build the JAR
```bash
mvn clean package -DskipTests
# Output: target/bookshelf-api-0.0.1-SNAPSHOT.jar
```

**What each flag does:**
- `clean` — deletes previous build output
- `package` — compiles and bundles into a JAR
- `-DskipTests` — skips tests for faster builds

### Step 2 — Dockerfile
Created in the root of the project:

```dockerfile
# Stage 1 — Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2 — Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/bookshelf-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why two stages (multi-stage build)?**
- Stage 1 uses full JDK to compile the app
- Stage 2 uses only JRE to run it — produces a smaller, leaner image (~180MB vs ~400MB)

### Step 3 — Build the image
```bash
docker build -t bookshelf-api:1.0 .
```

### Step 4 — Run as a container
```bash
docker run -p 8080:8080 bookshelf-api:1.0
```

**What `-p 8080:8080` means:**
- Maps port 8080 on your machine to port 8080 inside the container
- Format: `-p <host-port>:<container-port>`

### Step 5 — Push to Docker Hub
```bash
docker login

# Retag with your Docker Hub username
docker tag bookshelf-api:1.0 <username>/bookshelf-api:1.0

# Push
docker push <username>/bookshelf-api:1.0
```

Image is now publicly accessible. Anyone can run it with:
```bash
docker run -p 8080:8080 <username>/bookshelf-api:1.0
```

### Useful Docker commands
```bash
docker ps                  # list running containers
docker images              # list all images
docker stop <container-id> # stop a container
docker rm <container-id>   # remove a container
docker rmi <image-id>      # remove an image
```

### Key mental models
```
Dockerfile (recipe) → docker build → Image (snapshot) → docker run → Container (running instance)

Image is to Container what Class is to Object in Java.

Docker Hub is to Images what GitHub is to Code.
```

---

## Phase 3 — Kubernetes ☸️

### Why Kubernetes?
Docker runs one container on one machine. Kubernetes orchestrates containers at scale:
- Auto-restarts crashed containers
- Scales to multiple instances under load
- Handles rolling updates with zero downtime

### Three core concepts

| Concept | What it is |
|---------|-----------|
| **Pod** | Smallest unit — wraps one or more containers. One running instance of your app |
| **Deployment** | Manages Pods — which image, how many replicas, how to update |
| **Service** | Stable endpoint in front of Pods — Pods come and go, Service stays |

```
curl request → Service (stable) → Deployment → Pod 1 or Pod 2
```

### Installation (WSL)
```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
chmod +x minikube-linux-amd64 && sudo mv minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster using Docker driver
minikube start --driver=docker
```

### Project structure
```
bookshelf-api/
└── k8s/
    ├── deployment.yaml
    └── service.yaml
```

### k8s/deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bookshelf-api
spec:
  replicas: 2
  selector:
    matchLabels:
      app: bookshelf-api
  template:
    metadata:
      labels:
        app: bookshelf-api
    spec:
      containers:
        - name: bookshelf-api
          image: <username>/bookshelf-api:1.0
          ports:
            - containerPort: 8080
```

### k8s/service.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: bookshelf-api-service
spec:
  type: NodePort
  selector:
    app: bookshelf-api
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
      nodePort: 30080
```

### Service types

| Type | Use case |
|------|---------|
| `ClusterIP` | Only inside the cluster (default) |
| `NodePort` | Accessible from outside via node port — local dev |
| `LoadBalancer` | Cloud environments (AWS, GCP) — provisions real load balancer |

### Key commands
```bash
# Apply manifests
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Check status
kubectl get pods
kubectl get services

# Access the app
curl http://$(minikube ip):30080/books
```

### API groups — common gotcha
Different resources use different apiVersions. Using the wrong one throws "no matches for kind" error.

| Resource | apiVersion |
|----------|------------|
| Pod, Service, ConfigMap | `v1` |
| Deployment, ReplicaSet | `apps/v1` |
| Ingress | `networking.k8s.io/v1` |

### Request flow
```
curl → Minikube node (port 30080) → Service → Pod 1 or Pod 2 → Spring Boot app
```

---

## Phase 4 — CI/CD with Jenkins

### Why CI/CD?
Every step so far — building the JAR, building Docker image, pushing to Docker Hub, deploying to Kubernetes — was manual. CI/CD automates the entire chain on every code push.

```
Code push → Build JAR → Run tests → Build Docker image → Push to Docker Hub → Deploy to Kubernetes
```

### Installation
```bash
# Add Jenkins repo
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo gpg --dearmor -o /usr/share/keyrings/jenkins-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.gpg] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update && sudo apt install jenkins -y
sudo systemctl start jenkins
```

### Plugins required
- `Docker Pipeline`
- `Kubernetes CLI`

### Fix Docker permission for Jenkins
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# Verify
sudo -u jenkins docker ps
```

### Fix Kubernetes access for Jenkins
```bash
# Copy kubeconfig
sudo mkdir -p /var/lib/jenkins/.kube
sudo cp ~/.kube/config /var/lib/jenkins/.kube/config
sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube

# Copy minikube certs
sudo cp -r ~/.minikube /var/lib/jenkins/.minikube
sudo chown -R jenkins:jenkins /var/lib/jenkins/.minikube

# Fix cert paths
sudo sed -i "s|/home/$USER/.minikube|/var/lib/jenkins/.minikube|g" /var/lib/jenkins/.kube/config

# Verify
sudo -u jenkins kubectl get nodes
```

### Credentials setup
Go to **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

| Credential | Kind | ID |
|------------|------|----|
| Docker Hub | Username with password | `dockerhub-credentials` |
| GitHub | SSH Username with private key | `github-ssh` |

💡 Use a Docker Hub **Access Token** instead of password — safer and avoids auth issues.

### Jenkinsfile
```groovy
pipeline {
    agent any

    environment {
        IMAGE_NAME = "<your-dockerhub-username>/bookshelf-api"
        IMAGE_TAG  = "latest"
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Pulling latest code...'
                checkout scm
            }
        }

        stage('Build JAR') {
            steps {
                echo 'Building JAR with Maven...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                echo 'Pushing image to Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                    '''
                    sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo 'Deploying to Kubernetes...'
                sh 'kubectl apply -f k8s/deployment.yaml'
                sh 'kubectl apply -f k8s/service.yaml'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check logs above.'
        }
    }
}
```

### Key gotchas

| Issue | Fix |
|-------|-----|
| Branch not found | Change `*/master` to `*/main` in pipeline config |
| Docker permission denied | Add jenkins to docker group |
| kubectl validation error | Copy kubeconfig and minikube certs to jenkins user |
| Docker login fails | Use single quotes `'...'` for credential shell commands — double quotes cause Groovy interpolation |

### Pipeline flow
```
GitHub push → Jenkins pulls code → Maven builds JAR → Docker builds image → Push to Docker Hub → kubectl deploys to Minikube
```

---

## Phase 5 — AWS Deployment
*(coming soon)*