#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
if [ ! -r .env.production ]; then
  echo 'Missing .env.production (create it from deploy/.env.production.example)' >&2
  exit 1
fi

mode=${DEPLOY_DB_MODE:-}
if [ -z "$mode" ]; then
  mode=$(sed -n 's/^[[:space:]]*DEPLOY_DB_MODE[[:space:]]*=[[:space:]]*//p' .env.production | tail -n 1 | tr -d '\r')
fi
case "$mode" in
  ''|external) mode=external ;;
  container) ;;
  *) echo "DEPLOY_DB_MODE must be 'external' or 'container' (got '$mode')" >&2; exit 2 ;;
esac

if [ "${1:-}" = mode ]; then
  printf '%s\n' "$mode"
  exit 0
fi

if [ "$mode" = container ]; then
  set -- --env-file .env.production -f deploy/compose.production.yml -f deploy/compose.database.yml "$@"
else
  set -- --env-file .env.production -f deploy/compose.production.yml "$@"
fi
exec docker compose "$@"
