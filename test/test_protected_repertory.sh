#!/bin/bash

# Test that unauthenticated access to non-public repertories is denied for both
# repertory search and repertory browsing REST endpoints.
# This script selects a restricted repertory dynamically from the database and
# must not hard-code any specific repertory abbreviation.

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
echo "## Protected repertory access tests (unauthenticated)"
echo "## DB: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
echo "## Application server: ${OOREP_TEST_SERVER:-<unset>}"
echo "####################################################"

RESTRICTED_ABBREV="$(${psql_cmd[@]} -c "SELECT abbrev FROM info WHERE access NOT IN ('Public','Default') ORDER BY abbrev LIMIT 1;" 2>/dev/null | tr -d '[:space:]')"

if [ -z "$RESTRICTED_ABBREV" ]; then
    echo -e "${RED}SKIPPED${NC} - no restricted repertory found in database (or DB unavailable)."
    exit 0
fi

echo "## Using restricted repertory abbrev: [redacted in logs where possible]"
if [ -z "${OOREP_TEST_SERVER:-}" ]; then
    echo -e "${RED}WARNING${NC} - application server URL could not be determined (set OOREP_APP_PROTOCOL/OOREP_APP_HOSTNAME/OOREP_APP_DOMAIN or OOREP_TEST_SERVER); HTTP checks will fail to connect."
fi
echo

echo -n "- Unauthenticated repertory search is denied for restricted repertory...  "
status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_rep?repertory=${RESTRICTED_ABBREV}&symptom=head&page=0&remedyString=&minWeight=0&getRemedies=0")
print_result "$status" 204

echo -n "- Unauthenticated browse repertory (chapters) is denied for restricted repertory...  "
status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/browse_repertory?repertory=${RESTRICTED_ABBREV}")
print_result "$status" 204

echo -n "- Unauthenticated browse chapter is denied for restricted repertory...  "
status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/browse_chapter?repertory=${RESTRICTED_ABBREV}&chapterId=1")
print_result "$status" 204

echo -n "- Unauthenticated browse rubric remedies is denied for restricted repertory...  "
status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/browse_rubric_remedies?repertory=${RESTRICTED_ABBREV}&rubricId=1")
print_result "$status" 204

echo "####################################################"
