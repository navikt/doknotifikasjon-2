create table K_MOTTAKER_ID_TYPE
(
    K_MOTTAKER_ID_TYPE varchar(20) not null,
    primary key (K_MOTTAKER_ID_TYPE)
);

create table K_STATUS
(
    K_STATUS varchar(20) not null,
    primary key (K_STATUS)
);

create table K_KANAL
(
    K_KANAL varchar(20) not null,
    primary key (K_KANAL)
);

insert into K_KANAL(K_KANAL)
values ('SMS'),
       ('EPOST');

insert into K_MOTTAKER_ID_TYPE(K_MOTTAKER_ID_TYPE)
values ('FNR');

insert into K_STATUS(K_STATUS)
values ('OPPRETTET'),
       ('OVERSENDT'),
       ('FERDIGSTILT'),
       ('FEILET');

create table T_NOTIFIKASJON
(
    ID                        integer            not null,
    BESTILLINGS_ID            varchar(40) unique not null,
    BESTILLER_ID              varchar(40)        not null,
    MOTTAKER_ID               varchar(40)        not null,
    K_MOTTAKER_ID_TYPE        varchar(20)        not null
        constraint CONSTRAINT_MOTTAKER_ID_TYPE references K_MOTTAKER_ID_TYPE,
    K_STATUS                  varchar(20)        not null
        constraint CONSTRAINT_NOT_STATUS references K_STATUS,
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

create sequence NOTIFIKASJON_ID_SEQ cache 20;

create index IDX_STATUS_RENOTIFIKASJONER ON T_NOTIFIKASJON (K_STATUS, ANTALL_RENOTIFIKASJONER, NESTE_RENOTIFIKASJON_DATO);

create table T_NOTIFIKASJON_DISTRIBUSJON
(
    ID              integer       not null,
    NOTIFIKASJON_ID integer       not null,
    K_STATUS        varchar(20)   not null
        constraint CONSTRAINT_NOT_DISTR_STATUS references K_STATUS,
    K_KANAL         varchar(20)   not null
        constraint CONSTRAIN_KANAL references K_KANAL,
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

create sequence NOTIFIKASJON_DISTRIBUSJON_ID_SEQ cache 20;
