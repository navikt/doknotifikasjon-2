package no.nav.doknotifikasjon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "T_NOTIFIKASJON_DISTRIBUSJON")
public class NotifikasjonDistribusjon implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifikasjonDistribusjonIdSeq")
	@GenericGenerator(name = "notifikasjonDistribusjonIdSeq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "NOTIFIKASJON_DISTRIBUSJON_ID_SEQ")
	})
	@Column(name = "ID")
	private Integer notifikasjonDistribusjonId;

	@ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.LAZY)
	@JoinColumn(name = "NOTIFIKASJON_ID", foreignKey = @ForeignKey(name = "notifikasjonId"))
	private Notifikasjon notifikasjonId;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_STATUS", length = 20)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_KANAL", length = 20)
	private Kanal kanal;

	@Column(name = "KONTAKT_INFO", length = 255)
	private String kontaktInfo;

	@Column(name = "TITTEL", length = 40)
	private String tittel;

	@Column(name = "TEKST", length = 4000)
	private String tekst;

	@Column(name = "SENDT_DATO")
	private LocalDateTime sendtDato;

	@Column(name = "OPPRETTET_AV", length = 40)
	private String opprettetAv;

	@Column(name = "OPPRETTET_DATO")
	private LocalDateTime opprettetDato;

	@Column(name = "ENDRET_AV", length = 40)
	private String endretAv;

	@Column(name = "ENDRET_DATO")
	private LocalDateTime endretDato;
}
