package no.nav.doknotifikasjon.model;

import lombok.*;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_NOTIFIKASJON_DISTRIBUSJON")
public class NotifikasjonDistribusjon implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifikasjonDistribusjonIdSeq")
	@SequenceGenerator(name = "notifikasjonDistribusjonIdSeq", sequenceName = "NOTIFIKASJON_DISTRIBUSJON_ID_SEQ", allocationSize = 1)
	@Column(name = "ID")
	private Integer id;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "NOTIFIKASJON_ID", foreignKey = @ForeignKey(name = "notifikasjonId"))
	private Notifikasjon notifikasjon;

	@Column(name = "leverandor_ordre_id", length = 128)
	private String leverandorOrdreId;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_STATUS", length = 20)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_KANAL", length = 20)
	private Kanal kanal;

	@Column(name = "KONTAKT_INFO", length = 255)
	private String kontaktInfo;

	@Column(name = "TITTEL", length = 100)
	private String tittel;

	@Column(name = "TEKST", length = 4000)
	private String tekst;

	@Column(name = "SENDT_DATO")
	private LocalDateTime sendtDato;

	@Column(name = "OPPRETTET_AV", length = 100)
	private String opprettetAv;

	@Column(name = "OPPRETTET_DATO")
	private LocalDateTime opprettetDato;

	@Column(name = "ENDRET_AV", length = 100)
	private String endretAv;

	@Column(name = "ENDRET_DATO")
	private LocalDateTime endretDato;

	public void setAltinnNotificationOrderId(UUID altinnNotificationOrderId) {
		this.leverandorOrdreId = altinnNotificationOrderId.toString();
	}
}
