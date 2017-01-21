#!/usr/bin/env bash

if [[ $KEYSTORE_FOLDER && ${KEYSTORE_FOLDER} && $KEYSTORE_URI_PUB && ${KEYSTORE_URI_PUB} && \
    $KEYSTORE_URI_PRIV && ${KEYSTORE_URI_PRIV} ]]
then
    echo "Downloading PGP file..."
    mkdir -p ${KEYSTORE_DIR}
    curl -L -o ${KEYSTORE_DIR}/pubring.asc ${KEYSTORE_URI_PUB}
    curl -L -o ${KEYSTORE_DIR}/secring.asc ${KEYSTORE_URI_PRIV}
else
    echo "Keystore uri(s) not set. No PGP signing available."
fi
