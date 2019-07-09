

REPORT_DIR=test-tmp
if [ ! -d $REPORT_DIR ]; then
    mkdir $REPORT_DIR
fi

URL=http://localhost:5050


function curl-test() {
    TEST_NAME=$1
    METHOD=$2
    RESOURCE=$3
    EXPECTED_STATUS=$4

    URI=$URL/$RESOURCE
    FILE=$REPORT_DIR/$$_$TEST_NAME.out

    # redirct stderr to suppress network stats
    curl -i -X $METHOD $URI 2>/dev/null > $FILE
    STATUS=`cat $FILE | grep HTTP/1.1 | awk '{ print $2 }'`
    BODY=`cat $FILE | tail -n +5`

    if [ $EXPECTED_STATUS == $STATUS ]; then
        echo "PASS - $TEST_NAME : $METHOD $URI $STATUS"
    else
        echo "FAIL - $TEST_NAME : $METHOD $URI - expected $EXPECTED_STATUS, received $STATUS"
    fi
    #echo "    body:   $BODY"
}


curl-test products GET products 200
curl-test products GET products 202
