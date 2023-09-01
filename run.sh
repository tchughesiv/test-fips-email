#!/bin/bash

if [ "$#" -ne 4 ]; then
    echo "Usage: $0 SMTP_USER SMTP_PWD TRUSTSTORE_PWD EMAIL"
    echo "This script expects exactly 4 arguments."
    exit 1
fi

SMTP_USER="$1"
SMTP_PWD="$2"
TRUSTSTORE_PWD="$3"
EMAIL="$4"

if [ ! -d "target" ]; then
    # Build only once
    # If you update the Java code either remove build it manually or remove the target folder
    mvn clean install
else
    echo "Skipping Maven build - remove target folder to force build"
fi

# NON FIPS endpoint
# SMTP=email-smtp.us-gov-west-1.amazonaws.com
# FIPS endpoint
SMTP=email-smtp-fips.us-gov-west-1.amazonaws.com

echo "Running with EXTRA_OPTS=$EXTRA_OPTS"

java -Djava.security.debug=properties \
-agentlib:jdwp=transport=dt_socket,address=5007,server=y,suspend=n \
-Djavax.net.ssl.trustStore=$PWD/tls/cacerts.bcfks \
-Djavax.net.ssl.trustStoreType=bcfks \
-Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PWD} \
$EXTRA_OPTS \
-jar target/fips-email-test-0.1-SNAPSHOT.jar \
${SMTP} ${SMTP_USER} ${SMTP_PWD} ${EMAIL}