#!/usr/bin/env bash

if [[ $KEYSTORE && ${KEYSTORE} && $KEYSTORE_URI && ${KEYSTORE_URI} ]]
then
    echo "Downloading PGP file..."
    KEYSTORE_DIR="$(dirname $KEYSTORE)"
    mkdir -p ${KEYSTORE_DIR}
    curl -L -o ${KEYSTORE} ${KEYSTORE_URI}
else
    echo "Keystore uri not set. No PGP signing available."
fi