#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdoknotifikasjon-2/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdoknotifikasjon-2/username)
fi

if test -f /var/run/secrets/nais.io/srvdoknotifikasjon-2/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdoknotifikasjon-2/password)
fi