# Doknotifikasjon-2
Doknotifikasjon-2 er ansvarlig for å sende notifikasjon (varsel) til mottaker via epost eller sms.

Appen leser meldinger fra Kafka-topicene `teamdokumenthandtering.privat-dok-notifikasjon`og `teamdokumenthandtering.privat-dok-notifikasjon-med-kontakt-info`,
og ruter dette videre til Kafka-topicene `teamdokumenthandtering.privat-dok-notifikasjon-sms` og `teamdokumenthandtering.privat-dok-notifikasjon-epost` som potensielt gir en notifikasjon på henholdsvis sms eller epost.
[Avro-skjema for meldinger som blir produsert og konsumert](https://github.com/navikt/teamdokumenthandtering-avro-schemas) av appen ligger på Github.

For mer informasjon om appen kan du se på [funksjonell beskrivelse på confluence](https://confluence.adeo.no/display/BOA/doknotifikasjon+-+Funksjonell+Beskrivelse).
 
Merk at en tidligere versjon av appen som brukte onprem-Kafka, [doknotifikasjon](https://github.com/navikt/doknotifikasjon), nå har blitt erstattet av doknotifikasjon-2 som bruker managed Kafka (Aiven). 

## Deploy av appen
Distribusjon av tjenesten er gjort med integrasjon mot Jenkins:
[doknotifikasjon CI / CD](https://dok-jenkins.adeo.no/job/doknotifikasjon/job/master/)

Push/merge til master branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

Avro skjemaløsning blir brukt for å sørge for at det vi consumer og produser på kafka topic har specefikt struktur. Hvis ikke Avro blir brukt, så blir det sendt et JSON object. Avro skjema se til dette repo: [avro skjema](https://github.com/navikt/doknotifikasjon-schemas)

## Henvendelser
Spørsmål om koden eller prosjektet kan rettes til [Slack-kanalen for \#Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)