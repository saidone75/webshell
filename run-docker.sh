#!/usr/bin/env bash
set -euo pipefail

# Resolve project paths relative to this script, even when called elsewhere.
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

APP_NAME=webshell
COMPOSE_SERVICE=webshell
HARBOR_URL=harbor.tai.it/webshell
COMPOSE_CMD=()
export JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75

usage() {
    echo "Usage: ${0##*/} {build|push|build_start|start|stop|restart|purge|tail|update}"
}

COMMAND=${1:-}
case "${COMMAND,,}" in
    build|push|build_start|start|stop|restart|purge|tail|update) ;;
    *) usage >&2; exit 1 ;;
esac

detect_compose() {
    if docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD=(docker compose)
    elif command -v docker-compose >/dev/null 2>&1 && docker-compose version >/dev/null 2>&1; then
        COMPOSE_CMD=(docker-compose)
    else
        echo "ERROR: Docker Compose is unavailable. Install the Docker Compose plugin (docker compose) or docker-compose." >&2
        exit 1
    fi
    COMPOSE_CMD+=(-f docker/docker-compose.yml)
}

# Check Compose before performing any build or volume/container operations.
case "${COMMAND,,}" in
    build|push) ;;
    *) detect_compose ;;
esac

read_version() {
    local version
    if ! version=$(mvn -q -Dstyle.color=never help:evaluate \
        -Dexpression=project.version -DforceStdout); then
        echo "ERROR: cannot read version from pom.xml" >&2
        exit 1
    fi
    # Some Maven installations emit ANSI reset sequences even with color disabled.
    APP_VERSION=$(printf '%s' "$version" | sed $'s/\033\\[[0-9;]*m//g' | tr -d '\r')
    if [[ ! "$APP_VERSION" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]]; then
        echo "ERROR: invalid or empty project version: $APP_VERSION" >&2
        exit 1
    fi
    echo "Project version: $APP_VERSION"
}

build() {
    docker build -t "$APP_NAME:$APP_VERSION" . -f docker/Dockerfile
    docker tag "$APP_NAME:$APP_VERSION" "$APP_NAME:latest"
}

push() {
    docker tag "$APP_NAME:$APP_VERSION" "$APP_NAME:latest"
    docker tag "$APP_NAME:$APP_VERSION" "$HARBOR_URL/$APP_NAME:$APP_VERSION"
    docker push "$HARBOR_URL/$APP_NAME:$APP_VERSION"
}

start() {
    "${COMPOSE_CMD[@]}" up -d
}

down() {
    "${COMPOSE_CMD[@]}" down
}

tail_logs() {
    "${COMPOSE_CMD[@]}" logs -f
}

update() {
    build
    "${COMPOSE_CMD[@]}" stop "$COMPOSE_SERVICE"
    "${COMPOSE_CMD[@]}" rm -f "$COMPOSE_SERVICE"
    "${COMPOSE_CMD[@]}" up -d --no-build "$COMPOSE_SERVICE"
    tail_logs
}

# Maven is only needed for commands that use an image version.
case "${COMMAND,,}" in
    build|push|build_start|update) read_version ;;
esac

case "${COMMAND,,}" in
    build) build ;;
    push) push ;;
    build_start) build; start; tail_logs ;;
    start) start; tail_logs ;;
    stop) down ;;
    restart) down; start; tail_logs ;;
    purge) down; "${COMPOSE_CMD[@]}" down --rmi local --remove-orphans ;;
    tail) tail_logs ;;
    update) update ;;
esac
