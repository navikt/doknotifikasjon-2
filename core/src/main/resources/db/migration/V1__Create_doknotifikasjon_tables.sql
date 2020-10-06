create table T_NOTIFIKASJON
(
    ID                        integer            not null,
    BESTILLING_ID             varchar(40) unique not null,
    BESTILLER_ID              varchar(40)        not null,
    MOTTAKER_ID               varchar(40)        not null,
    K_MOTTAKER_ID_TYPE        varchar(20)        not null,
    K_STATUS                  varchar(20)        not null,
    ANTALL_RENOTIFIKASJONER   integer,
    RENOTIFIKASJON_INTERVALL  integer,
    NESTE_RENOTIFIKASJON_DATO date,
    PREFERERTE_KANALER        varchar(20),
    OPPRETTET_AV              varchar(40)        not null,
    OPPRETTET_DATO            timestamp          not null,
    ENDRET_AV                 varchar(40),
    ENDRET_DATO               timestamp,

    primary key (ID)
);

create sequence NOTIFIKASJON_ID_SEQ; --todo

create index IDX_StatusAntallRenotifikasjonerNesteRenotifikasjonDato ON T_NOTIFIKASJON (K_STATUS, ANTALL_RENOTIFIKASJONER, NESTE_RENOTIFIKASJON_DATO); --todo

create table T_NOTIFIKASJON_DISTRIBUSJON
(
    ID              integer       not null,
    NOTIFIKASJON_ID integer       not null,
    K_STATUS        varchar(20)   not null,
    K_KANAL         varchar(20)   not null,
    KONTAKT_INFO    varchar(255)  not null,
    TITTEL          varchar(40)   not null,
    TEKST           varchar(4000) not null,
    SENDT_DATO      timestamp,
    OPPRETTET_AV    varchar(40)   not null,
    OPPRETTET_DATO  timestamp     not null,
    ENDRET_AV       varchar(40),
    ENDRET_DATO     timestamp,

    primary key (ID),
    foreign key (NOTIFIKASJON_ID) references T_NOTIFIKASJON (ID)
);

CREATE SEQUENCE NOTIFIKASJON_DISTRIBUSJON_ID_SEQ;

CREATE TABLE K_MOTTAKER_ID_TYPE
(
    K_MOTTAKER_ID_TYPE VARCHAR(20) NOT NULL,

    PRIMARY KEY (K_MOTTAKER_ID_TYPE)
);

CREATE TABLE K_STATUS
(
    K_STATUS VARCHAR(20) NOT NULL,

    PRIMARY KEY (K_STATUS)
);

CREATE TABLE K_KANAL
(
    K_KANAL VARCHAR(20) NOT NULL,

    PRIMARY KEY (K_KANAL)
);

INSERT INTO K_MOTTAKER_ID_TYPE(K_MOTTAKER_ID_TYPE)
VALUES ('FNR');

INSERT INTO K_STATUS(K_STATUS)
VALUES ('OPPRETTET');
INSERT INTO K_STATUS(K_STATUS)
VALUES ('OVERSENDT');
INSERT INTO K_STATUS(K_STATUS)
VALUES ('FERDIGSTILT');
INSERT INTO K_STATUS(K_STATUS)
VALUES ('FEILET');

INSERT INTO K_KANAL(K_KANAL)
VALUES ('SMS');
INSERT INTO K_KANAL(K_KANAL)
VALUES ('EPOST');
