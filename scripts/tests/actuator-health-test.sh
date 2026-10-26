#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)

grep -Fq 'include: health' "$root_dir/backend/src/main/resources/application.yaml"
grep -Fq 'show-details: never' "$root_dir/backend/src/main/resources/application.yaml"
grep -Fq 'curl --fail --silent --show-error' "$root_dir/backend/docker-compose.yml"
grep -Fq 'http://127.0.0.1:$APP_PORT/actuator/health' "$root_dir/scripts/deploy-backend.sh"

echo 'actuator health contract test: PASS'
