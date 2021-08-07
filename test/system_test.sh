#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# First arg is the test result, second arg is the expected value.
print_result() {
    if [ $1 -eq $2 ]; then
	echo -e "${GREEN}Ok${NC}"
    else
	echo -e "${RED}FAILED${NC}"
    fi
}

echo "####################################################"
echo "## OOREP SYSTEM AND API-ENDPOINT TESTING"
echo "## Settings:"
echo "## - Application server: $OOREP_TEST_SERVER"
echo "## - Existing email: $OOREP_TEST_EXISTING_EMAIL"
echo "####################################################"

echo "- I can request username for $OOREP_TEST_EXISTING_EMAIL...  [check your mail]"
input_email="inputEmail=$OOREP_TEST_EXISTING_EMAIL"
curl --data "$input_email" -k "$OOREP_TEST_SERVER/api/request_username"

echo -n "- I don't get error for requesting username for non-existing email...  "
test=`curl --data 'inputEmail=sdfsdfsdf@blabla.org' -k "$OOREP_TEST_SERVER/api/request_username" 2>/dev/null`
test=`echo "$test" | grep -e "^.+$" | wc -l`
print_result $test 0

echo -n "- Return code for password change request for existing and non-existing email is the same...  "
input_email="inputEmail=$OOREP_TEST_EXISTING_EMAIL"
test1=`curl --write-out %{http_code} --data 'inputEmail=addfsfsdfsfd@blabla.org' -k "$OOREP_TEST_SERVER/api/request_username" 2>/dev/null`
test2=`curl --write-out %{http_code} --data "$input_email" -k "$OOREP_TEST_SERVER/api/request_username" 2>/dev/null`
print_result $test1 $test2

echo -n "- Cannot change password without password change request...  "
test=`curl --data 'pass1=23434F@343523dfvds23J' --data "pcrId=1" --data "memberId=1" -k "$OOREP_TEST_SERVER/api/submit_new_password" 2>/dev/null | grep failed | wc -l`
print_result $test 1

echo -n "- Call to /api/sec/ leads to HTTP-status 303, i.e., redirect to SP...  "
test=`curl -k --write-out %{http_code} "$OOREP_TEST_SERVER/api/sec/file?fileId=1" 2>/dev/null | grep ^303$ | wc -l`
print_result $test 1

echo -n "- Cannot manually set X-Remote-User in request...  "
test=`curl -H "X-Remote-User: 1" -v -k  "$OOREP_TEST_SERVER/api/authenticate" 2>/dev/null | grep "cannot be authenticated" | wc -l`
print_result $test 1

echo -n "- Lookup of thumb in publicum is unprotected and has 279 results...  "
test=`curl -k "$OOREP_TEST_SERVER/api/lookup_rep?repertory=publicum&symptom=thumb&page=1&remedyString=&minWeight=1&getRemedies=0" 2>/dev/null | grep totalNumberOfResults.*279 | wc -l`
print_result $test 1

echo -n "- Calling of /show?... yields HTTP-status 200...  "
test=`curl -k --write-out %{http_code} "$OOREP_TEST_SERVER/show?repertory=publicum&symptom=thumb&page=1&remedyString=&minWeight=1&getRemedies=0" 2>/dev/null | grep ^200$ | wc -l`
print_result $test 1

echo "####################################################"
