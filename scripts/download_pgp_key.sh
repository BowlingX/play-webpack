#!/usr/bin/env bash

if [[ $KEYSTORE_DIR && ${KEYSTORE_DIR} && $KEYSTORE_URI_PUB && ${KEYSTORE_URI_PUB} && \
    $KEYSTORE_URI_PRIV && ${KEYSTORE_URI_PRIV} ]]
then
    echo "Downloading PGP file..."
    mkdir -p ${KEYSTORE_DIR}
    curl -L -o ${KEYSTORE_DIR}/pubring.gpg ${KEYSTORE_URI_PUB}
    curl -L -o ${KEYSTORE_DIR}/secring.gpg ${KEYSTORE_URI_PRIV}
else
    echo "Keystore uri(s) not set. No PGP signing available."
fi
