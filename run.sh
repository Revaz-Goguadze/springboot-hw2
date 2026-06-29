#!/usr/bin/env bash
#
# Start the Task Manager application (one Spring Boot process serves the REST API,
# the Thymeleaf web UI, Swagger and Actuator — there is no separate frontend).
#
# Usage:
#   ./run.sh                 # dev profile (seeded H2), port 8080
#   ./run.sh dev 9090        # dev profile on a custom port
#   ./run.sh prod            # PostgreSQL — requires DB_URL / DB_USERNAME / DB_PASSWORD
#
set -euo pipefail
cd "$(dirname "$0")"

PROFILE="${1:-dev}"
PORT="${2:-8080}"

if [ "$PROFILE" = "prod" ] && [ -z "${DB_PASSWORD:-}" ]; then
  echo "ERROR: prod profile needs DB_PASSWORD (and usually DB_URL / DB_USERNAME) in the environment." >&2
  echo "  export DB_URL=jdbc:postgresql://localhost:5432/taskmanager" >&2
  echo "  export DB_USERNAME=taskmanager" >&2
  echo "  export DB_PASSWORD=yourpassword" >&2
  exit 1
fi

echo "Starting Task Manager  (profile=$PROFILE, port=$PORT)"
echo "  Web UI:    http://localhost:$PORT/"
echo "  Swagger:   http://localhost:$PORT/swagger-ui.html"
echo "  Actuator:  http://localhost:$PORT/actuator/health"
echo "  Login:     user / user123   |   admin / admin123"
[ "$PROFILE" = "dev" ] && echo "  (dev profile seeds sample users + tasks into in-memory H2)"
echo

# spring-boot:run compiles and launches via the Maven wrapper (no global Maven needed).
exec ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles="$PROFILE" \
  -Dspring-boot.run.jvmArguments="-Dserver.port=$PORT"
