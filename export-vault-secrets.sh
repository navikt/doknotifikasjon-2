#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdoknotifikasjon/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdoknotifikasjon/username)
fi

if test -f /var/run/secrets/nais.io/srvdoknotifikasjon/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdoknotifikasjon/password)
fi
