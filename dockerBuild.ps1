./gradlew :backend:assemble
./gradlew :frontend:app:jsBrowserProductionWebpack

$scriptPath = $MyInvocation.MyCommand.Path
$scriptDirectory = Split-Path $scriptPath -Parent

docker build -t ghcr.io/matsudamper/kake-bo:latest $scriptDirectory

docker login ghcr.io -u matsudamper -p $CR_PAT
docker push ghcr.io/matsudamper/kake-bo:latest
