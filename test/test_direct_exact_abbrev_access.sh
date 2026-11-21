#!/bin/bash

# Test direct REST access by exact resource abbreviation (unauthenticated):
# - public repertories and materia medicas must return results (200) for search
# - private/protected repertories and materia medicas must be denied (204) even
#   when their exact abbrev is known (not only hidden from overview lists)
# Prefers well-known examples (kent/synthesis, boericke/murphy) when present.

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
echo "## Direct exact-abbrev API access tests (unauthenticated)"
echo "## DB: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
echo "## Application server: ${OOREP_TEST_SERVER:-<unset>}"
echo "####################################################"

if [ -z "${OOREP_TEST_SERVER:-}" ]; then
    echo -e "${RED}WARNING${NC} - application server URL could not be determined (set OOREP_APP_PROTOCOL/OOREP_APP_HOSTNAME/OOREP_APP_DOMAIN or OOREP_TEST_SERVER); HTTP checks will fail to connect."
fi

pick_abbrev() {
    local preferred="$1"
    local query="$2"
    local found
    found="$(${psql_cmd[@]} -c "SELECT abbrev FROM (${query}) q WHERE abbrev='${preferred}' LIMIT 1;" 2>/dev/null | tr -d '[:space:]')"
    if [ -n "$found" ]; then
        echo "$found"
        return 0
    fi
    ${psql_cmd[@]} -c "${query} LIMIT 1;" 2>/dev/null | tr -d '[:space:]'
}

PUBLIC_REP=$(pick_abbrev "kent" "SELECT abbrev FROM info WHERE access IN ('Public','Default') ORDER BY abbrev")
PRIVATE_REP=$(pick_abbrev "synthesis" "SELECT abbrev FROM info WHERE access NOT IN ('Public','Default') ORDER BY abbrev")
PUBLIC_MM=$(pick_abbrev "boericke" "SELECT abbrev FROM mminfo WHERE access IN ('Public','Default') ORDER BY abbrev")
PRIVATE_MM=$(pick_abbrev "murphy" "SELECT abbrev FROM mminfo WHERE access NOT IN ('Public','Default') ORDER BY abbrev")

echo "## Public rep: ${PUBLIC_REP:-<none>}  Private rep: ${PRIVATE_REP:-<none>}"
echo "## Public MM:  ${PUBLIC_MM:-<none>}  Private MM:  ${PRIVATE_MM:-<none>}"
echo

# --- Repertories ---
if [ -n "$PUBLIC_REP" ]; then
    echo -n "- Direct repertory search returns 200 for public repertory (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_rep?repertory=${PUBLIC_REP}&symptom=head&page=0&remedyString=&minWeight=0&getRemedies=0")
    print_result "$status" 200
else
    echo -e "- Direct repertory search for public repertory...  ${RED}SKIPPED${NC} (none found)"
fi

if [ -n "$PRIVATE_REP" ]; then
    echo -n "- Direct repertory search is denied for private repertory (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_rep?repertory=${PRIVATE_REP}&symptom=head&page=0&remedyString=&minWeight=0&getRemedies=0")
    print_result "$status" 204

    echo -n "- Direct repertory browse is denied for private repertory (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/browse_repertory?repertory=${PRIVATE_REP}")
    print_result "$status" 204
else
    echo -e "- Direct repertory access for private repertory...  ${RED}SKIPPED${NC} (none found)"
fi

# --- Materia medicas ---
if [ -n "$PUBLIC_MM" ]; then
    echo -n "- Direct materia medica search returns 200 for public MM (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_mm?mmAbbrev=${PUBLIC_MM}&symptom=head&page=0&remedyString=")
    print_result "$status" 200
else
    echo -e "- Direct materia medica search for public MM...  ${RED}SKIPPED${NC} (none found)"
fi

if [ -n "$PRIVATE_MM" ]; then
    echo -n "- Direct materia medica symptom search is denied for private MM (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_mm?mmAbbrev=${PRIVATE_MM}&symptom=head&page=0&remedyString=")
    print_result "$status" 204

    echo -n "- Direct materia medica remedy search is denied for private MM (exact abbrev)...  "
    status=$(curl -s -k -o /dev/null -w '%{http_code}' "$OOREP_TEST_SERVER/api/lookup_mm?mmAbbrev=${PRIVATE_MM}&symptom=&page=0&remedyString=acon")
    print_result "$status" 204
else
    echo -e "- Direct materia medica access for private MM...  ${RED}SKIPPED${NC} (none found)"
fi

echo "####################################################"
