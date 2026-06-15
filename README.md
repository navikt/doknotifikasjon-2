# Doknotifikasjon-2
Doknotifikasjon-2 er ansvarlig for å sende notifikasjon (varsel) til mottaker via epost eller sms.

Appen leser meldinger fra Kafka-topicene `teamdokumenthandtering.privat-dok-notifikasjon`og `teamdokumenthandtering.privat-dok-notifikasjon-med-kontakt-info`,
og ruter dette videre til Kafka-topicene `teamdokumenthandtering.privat-dok-notifikasjon-sms` og `teamdokumenthandtering.privat-dok-notifikasjon-epost` som potensielt gir en notifikasjon på henholdsvis sms eller epost.
[Avro-skjema for meldinger som blir produsert og konsumert](https://github.com/navikt/teamdokumenthandtering-avro-schemas) av appen ligger på Github.

Appen tilbyr følgende tjenester, dokumentert i [swagger](https://doknotifikasjon-2.intern.dev.nav.no/swagger-ui/index.html): 
- (Kun til intern bruk) Henting av informasjon om notifikasjon [rnot001](https://confluence.adeo.no/display/BOA/RNOT001+-+NotifikasjonInfo)
- Henting av informasjon om en bruker kan varsles digitalt [rnot002](https://confluence.adeo.no/display/BOA/RNOT002+-+kanVarsles)

For mer informasjon om appen kan du se på [funksjonell beskrivelse på confluence](https://confluence.adeo.no/display/BOA/doknotifikasjon+-+Funksjonell+Beskrivelse).
 
Merk at en tidligere versjon av appen som brukte onprem-Kafka, [doknotifikasjon](https://github.com/navikt/doknotifikasjon), nå har blitt erstattet av doknotifikasjon-2 som bruker managed Kafka (Aiven). 

## Database fra lokal maskin
OBS! Krever naisdevice, og JITA til postgres-prod om du skal kjøre spørringer mot prod-databasen.

Database-URLer:

```
dev:   jdbc:postgresql://dev-pg.intern.nav.no:5432/doknotifikasjon-q2
prod:  jdbc:postgresql://prod-pg.intern.nav.no:5432/doknotifikasjon-p
```

Bruker og passord kan du hente fra [vault](https://vault.adeo.no/ui/vault/secrets) (klikk på "terminal" ikonet oppe i venstre hjørne og lim inn):

```
dev:   vault read postgresql/preprod-fss/creds/doknotifikasjon-q2-user
prod:  vault read postgresql/prod-fss/creds/doknotifikasjon-p-user
```

Eller via [vault-cli](https://github.com/navikt/vault-iac/blob/master/doc/vault-cli.md):

```
1. vault login -method=oidc
2. dev:  vault read postgresql/preprod-fss/creds/doknotifikasjon-q2-user
   prod: vault read postgresql/prod-fss/creds/doknotifikasjon-p-user
```

## Henvendelser
Spørsmål om koden eller prosjektet kan rettes til [Slack-kanalen for \#Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ).
