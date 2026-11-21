#!/bin/bash

# Test that unauthenticated access to non-public materia medicas is denied
# for direct REST lookups (even when the exact abbrev is known) and that they
# are not listed in the available materia medicas endpoint.
# Selects restricted materia medicas dynamically from the database.

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Derive test server from existing app variables if not explicitly provided.
if [ -z "${OOREP_TEST_SERVER:-}" ] && [ -n "${OOREP_APP_PROTOCOL:-}" ] && [ -n "${OOREP_APP_HOSTNAME:-}" ] && [ -n "${OOREP_APP_DOMAIN:-}" ]; then
    OOREP_TEST_SERVER="${OOREP_APP_PROTOCOL}://${OOREP_APP_HOSTNAME}.${OOREP_APP_DOMAIN}"
fi
OOREP_TEST_SERVER="${OOREP_TEST_SERVER%/}"

print_result() {
    if [ "$1" -eq "$2" ] 2>/dev/null; then
        echo -e "${GREEN}Ok${NC}"
    elif [ "$1" = "000" ] || [ -z "$1" ]; then
        echo -e "${RED}FAILED${NC} (got: ${1:-<empty>}, expected: $2; no HTTP response - is the application server running/reachable at ${OOREP_TEST_SERVER:-<unset>}?)"
    else
        echo -e "${RED}FAILED${NC} (got: $1, expected: $2)"
    fi
}

DB_HOST="${OOREP_TEST_DB_HOST:-localhost}"
DB_PORT="${OOREP_TEST_DB_PORT:-5001}"
DB_NAME="${OOREP_TEST_DB_NAME:-oorep_opencode_copy}"
DB_USER="${OOREP_TEST_DB_USER:-oorep_user}"

if [ -z "${PGPASSWORD:-}" ] && [ -n "${OOREP_DB_PASS:-}" ]; then
    export PGPASSWORD="$OOREP_DB_PASS"
fi

psql_cmd=(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tA)

echo "####################################################"
echo "## Protected materia medica access tests (unauthenticated)"
echo "## DB: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
echo "## Application server: ${OOREP_TEST_SERVER:-<unset>}"
echo "####################################################"

mapfile -t RESTRICTED_MMS < <(${psql_cmd[@]} -c "SELECT abbrev FROM mminfo WHERE access NOT IN ('Public','Default') ORDER BY abbrev;" 2>/dev/null | sed '/^$/d')

if [ ${#RESTRICTED_MMS[@]} -eq 0 ]; then
    echo -e "${RED}SKIPPED${NC} - no restricted materia medica found in database (or DB unavailable)."
    echo "####################################################"
    return 0 2>/dev/null || exit 0
fi

if [ -z "${OOREP_TEST_SERVER:-}" ]; then
    echo -e "${RED}WARNING${NC} - application server URL could not be determined (set OOREP_APP_PROTOCOL/OOREP_APP_HOSTNAME/OOREP_APP_DOMAIN or OOREP_TEST_SERVER); HTTP checks will fail to connect."
fi

echo "## Found ${#RESTRICTED_MMS[@]} restricted materia medica(s): [redacted in logs where possible]"
echo

available_mms=$(curl -s -k "$OOREP_TEST_SERVER/api/available_rems_and_mms" 2>/dev/null)

for mm in "${RESTRICTED_MMS[@]}"; do
    echo -n "- Unauthenticated lookup_mm (symptom search) is denied for restricted MM...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_mm?mmAbbrev=${mm}&symptom=head&page=0&remedyString=")
    print_result "$status" 204

    echo -n "- Unauthenticated lookup_mm (remedy search) is denied for restricted MM...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_mm?mmAbbrev=${mm}&symptom=&page=0&remedyString=acon")
    print_result "$status" 204

    echo -n "- Restricted MM is not listed in available_rems_and_mms (unauthenticated)...  "
    if [ -z "$available_mms" ]; then
        echo -e "${RED}FAILED${NC} (could not retrieve available materia medicas)"
    elif echo "$available_mms" | grep -q "\"abbrev\":\"${mm}\""; then
        echo -e "${RED}FAILED${NC} (found \"abbrev\":\"${mm}\" in available list)"
    else
        echo -e "${GREEN}Ok${NC}"
    fi
done

echo "####################################################"
