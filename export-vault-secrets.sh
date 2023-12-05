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


echo "Exporting appdynamics environment variables"
if test -f /var/run/secrets/nais.io/appdynamics/appdynamics.env;
then
    export $(cat /var/run/secrets/nais.io/appdynamics/appdynamics.env)
    export APPDYNAMICS_AGENT_BASE_DIR=/tmp/appdynamics
    echo "Appdynamics environment variables exported"
else
    echo "No such file or directory found at /var/run/secrets/nais.io/appdynamics/appdynamics.env"
fi

export new_credentials_2023_path=/secrets/virksomhetssertifikat/credentials_2023.json
export old_credentials_path=/secrets/virksomhetssertifikat/credentials.json

if test -f $new_credentials_2023_path
then
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat $new_credentials_2023_path | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat $new_credentials_2023_path | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat $new_credentials_2023_path | jq -r '.type')"
else
    echo "Setting virksomhetssertifikat_alias"
    export virksomhetssertifikat_alias="$(cat $old_credentials_path | jq -r '.alias')"
    echo "Setting virksomhetssertifikat_password"
    export virksomhetssertifikat_password="$(cat $old_credentials_path | jq -r '.password')"
    echo "Setting virksomhetssertifikat_type"
    export virksomhetssertifikat_type="$(cat $old_credentials_path | jq -r '.type')"

fi

if test -f /secrets/virksomhetssertifikat/274258896775237957919470-2023-10-11.p12.b64
then
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file:///secrets/virksomhetssertifikat/key.p12"

    echo "Converting certificate from base64"
    base64 --decode /secrets/virksomhetssertifikat/274258896775237957919470-2023-10-11.p12.b64 > /secrets/virksomhetssertifikat/key.p12
else
    echo "Setting virksomhetssertifikat_path"
    export virksomhetssertifikat_path="file:///secrets/virksomhetssertifikat/key.p12"

    echo "Converting certificate from base64"
    base64 --decode /secrets/virksomhetssertifikat/key.p12.b64 > /secrets/virksomhetssertifikat/key.p12
fi