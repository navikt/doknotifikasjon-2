# Doknotifikasjon

* [1. Funksjonelle Krav](#1-funksjonelle-krav)
* [2. Begrensninger](#2-begrensninger)
* [3. Distribusjon av tjenesten (deployment)](#3-distribusjon-av-tjenesten-deployment)
* [4. Utviklingsmiljø](#4-utviklingsmilj)
* [5. Drift og støtte](#5-drift-og-sttte)


# 1. Funksjonelle Krav
Tjenesten skal plukke hendelser fra kafka-topic dok-eksternnotifikasjon. Hendelsene formatteres med Avro, med skjemastøtte.

Tilgangsstyring må kunne begrense hvilke producere som kan skrive hendelser til topic.

For mer informasjon: [confluence](https://confluence.adeo.no/display/BOA/doknotifikasjon+-+Funksjonell+Beskrivelse)

# 2. Begrensninger
tjenesten har ikke noe forhold på innholdt til feltet epostTekst og smsTekst.

# 3. Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort med integrasjon mot Jenkins:
[doknotifikasjon CI / CD](https://dok-jenkins.adeo.no/job/doknotifikasjon/job/master/)

Push/merge til master branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

# 4. Utviklingsmiljø
## Forutsetninger
* docker
* docker-compose
* Java 11
* Kubectl


## Kjøre Prosjekt
Lokal utvikling er satt opp slik at applikasjonen kjøres lokalt og kobles til kafka miljø via et oppsett på docker. 
For å kjøre opp applikasjonen lokal, bruk profile `local` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/doknotifikasjon) 
For å sette `VAULT_TOKEN` lokalt må du logge deg inn på [vault](https://vault.adeo.no/ui/vault/secrets) og gå til nedtrekslisten til brukerprofilen. Velg `copy token` og legg til token i systemvariablene.

For å sette opp Kafka docker compose, skriv inn disse kommandoene under folder ~./docker:
```shell script
docker-compose build
docker-compose up
```

### Produsere kafka meldinger
For å produsere kafka meldinger, må man først exec inn på kafka kontaineren ved å bruker docker dashbord, eller ved å kjøre følgende kommando:
```shell script
docker exec -it <container-id til kafka> /bin/sh; exit
```

Deretter, kjøre følgende kommando for å koble til kafka instansen:
```shell script
kafka-console-producer --broker-list localhost:9092 --topic <topic> --producer.config=$CLASSPATH/producer.properties
```

Avro skjemaløsning blir brukt for å sørge for at det vi consumer og produser på kafka topic har specefikt struktur. Hvis ikke Avro blir brukt, så blir det sendt et JSON object. Avro skjema se til dette repo: [avro skjema](https://github.com/navikt/doknotifikasjon-schemas)

# 5. Drift og støtte
## Logging
Loggene til tjenesten kan leses på to måter:

### Kibana
For [dev-fss](https://logs.adeo.no/goto/3d51098ce277cc4ddf74d8a099f9444b)

For [prod-fss](https://logs.adeo.no/goto/d3fec3fd86d445c76ec5f5bc33c77cf7)

### Kubectl
For dev-fss:
```shell script
kubectl config use-context dev-fss
kubectl get pods -n q1 | grep doknotifikasjon
kubectl logs -f doknotifikasjon-<POD-ID> -n teamdokumenthandtering -c doknotifikasjon
```

For prod-fss:
```shell script
kubectl config use-context prod-fss
kubectl get pods -n p | grep doknotifikasjon
kubectl logs -f doknotifikasjon-<POD-ID> -n teamdokumenthandtering -c doknotifikasjon
```

## Metrics

## Henvendelser
Spørsmål koden eller prosjekttet kan rettes til Team Dokumentløsninger på:
* [\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)
