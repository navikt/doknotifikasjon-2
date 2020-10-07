package no.nav.doknotifikasjon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.doknotifikasjon.kodeverk.Kanal;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
	@Column(name = "notifikasjonDistribusjonId")
	private Integer notifikasjonDistribusjonId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notifikasjonId", foreignKey = @ForeignKey(name = "notifikasjonId"))
	private Notifikasjon notifikasjonId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(name = "kanal", length = 20)
	private Kanal kanal;

	@Column(name = "kontaktInfo", length = 255)
	private String kontaktInfo;

	@Column(name = "tittel", length = 40)
	private String tittel;

	@Column(name = "tekst", length = 4000)
	private String tekst;

	@Column(name = "sendtDato")
	private LocalDateTime sendtDato;

	@Column(name = "opprettetAv", length = 40)
	private String opprettetAv;

	@Column(name = "opprettetDato")
	private LocalDateTime opprettetDato;

	@Column(name = "endretAv", length = 40)
	private String endretAv;

	@Column(name = "endretDato")
	private LocalDateTime endretDato;
}
