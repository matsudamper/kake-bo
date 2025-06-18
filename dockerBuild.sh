#!/bin/bash
PATH=$PATH:/home/matsudamper/.jdks/temurin-17.0.8/bin

./gradlew :backend:assemble
./gradlew :frontend:app:jsBrowserProductionWebpack

docker build -t ghcr.io/matsudamper/kake-bo:latest .

docker login ghcr.io -u matsudamper -p $CR_PAT
docker push ghcr.io/matsudamper/kake-bo:latest
