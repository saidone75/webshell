# Web Shell

Web Shell provides a browser-based terminal backed by a server-side shell process. The browser uses [xterm.js](https://xtermjs.org/) for terminal rendering and a WebSocket connection for interactive input and output.

## Features

- Interactive terminal available from a web browser
- WebSocket endpoint at `/shell`
- Password-protected WebSocket handshake
- Automatic terminal resizing
- PowerShell support on Windows
- POSIX shell support on Linux and other Unix-like systems
- Local Maven and Docker-based execution

## Requirements

For local development:

- Java 17 or newer
- Maven 3.6.3 or newer

For containerized execution:

- Docker
- Docker Compose

## Configuration

The application listens on port `8080` by default. The shell password is configured with the `SHELL_PASSWORD` environment variable:

```text
SHELL_PASSWORD=change-this-password
```

If the variable is not set, the application uses the default value defined in `src/main/resources/application.yml`. Set an explicit, strong password before exposing the application outside a trusted local environment.

The port can be changed with the standard Spring Boot property:

```text
SERVER_PORT=9090
```

## Running locally

On Windows, use the included script:

```bat
run.bat
```

Alternatively, start the application with Maven:

```bash
mvn spring-boot:run
```

To provide a password for the current session on PowerShell:

```powershell
$env:SHELL_PASSWORD = "change-this-password"
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) and enter the configured password.

## Building

Build the WAR file with:

```bash
mvn clean package -DskipTests -Dlicense.skip=true
```

On Windows, the equivalent helper script is:

```bat
build.bat
```

The artifact is written to `target/Web Shell-1.0.0.war`.

## Running with Docker Compose

Build and start the container from the project root:

```bash
docker compose -f docker/docker-compose.yml up --build
```

The application is then available at [http://localhost:8080](http://localhost:8080). Set `SHELL_PASSWORD` when starting the container if you want to override the configured password:

```bash
SHELL_PASSWORD="change-this-password" docker compose -f docker/docker-compose.yml up --build
```

On Windows, `run-docker.bat` provides commands for building, starting, stopping, updating, and viewing logs:

```bat
run-docker.bat build_start
```

Run the script without arguments to display the available commands.

## WebSocket protocol

The browser connects to the shell using:

```text
ws://localhost:8080/shell?password=<password>
```

Use `wss://` when the application is served over HTTPS. Text messages are forwarded to the shell process. Terminal dimensions are sent in the following format:

```text
resize:<columns>:<rows>
```

Each WebSocket connection creates one shell process. The process is forcibly terminated when the connection closes.

## Security considerations

Web Shell executes commands on the host running the application with the permissions of the application process. Treat it as an administrative tool and run it only in a trusted environment. Use a strong password, restrict network access, and place it behind HTTPS and an appropriate reverse proxy before making it available over an untrusted network.

The WebSocket endpoint currently allows requests from any origin. Additional network controls and authentication should be added for production deployments.

## Project layout

```text
src/main/java/                         Application and WebSocket implementation
src/main/resources/application.yml     Spring Boot configuration
src/main/resources/static/shell.html   Browser terminal client
docker/Dockerfile                       Multi-stage container build
docker/docker-compose.yml               Docker Compose configuration
```
