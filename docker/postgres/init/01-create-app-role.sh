#!/bin/sh
set -eu

: "${DB_APP_USER:?DB_APP_USER must be set}"
: "${DB_APP_PASSWORD:?DB_APP_PASSWORD must be set}"

if [ "$DB_APP_USER" = "$POSTGRES_USER" ]; then
    echo "DB_APP_USER must differ from POSTGRES_USER" >&2
    exit 1
fi

if [ "$DB_APP_PASSWORD" = "$POSTGRES_PASSWORD" ]; then
    echo "DB_APP_PASSWORD must differ from POSTGRES_PASSWORD" >&2
    exit 1
fi

psql --set=ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=app_user="$DB_APP_USER" \
  --set=app_password="$DB_APP_PASSWORD" <<'SQL'
SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION',
    :'app_user',
    :'app_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

SELECT format('GRANT CONNECT, TEMPORARY ON DATABASE %I TO %I', current_database(), :'app_user')
\gexec

SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'app_user')
\gexec
SQL

role_security="$(
    psql --set=ON_ERROR_STOP=1 \
      --username "$POSTGRES_USER" \
      --dbname "$POSTGRES_DB" \
      --set=app_user="$DB_APP_USER" \
      --tuples-only --no-align <<'SQL'
SELECT concat_ws(',', rolsuper, rolcreatedb, rolcreaterole, rolreplication, rolbypassrls, rolcanlogin)
FROM pg_roles
WHERE rolname = :'app_user';
SQL
)"

if [ "$role_security" != "f,f,f,f,f,t" ]; then
    echo "Application database role is missing or has elevated cluster privileges" >&2
    exit 1
fi
