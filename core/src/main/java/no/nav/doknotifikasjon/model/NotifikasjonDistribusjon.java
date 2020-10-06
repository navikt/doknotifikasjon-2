package no.nav.doknotifikasjon.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@AllArgsConstructor
@Table(name = "T_NOTIFIKASJON_DISTRIBUSJON")
public class NotifikasjonDistribusjon implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifikasjonDistribusjonIdSeq")
	@GenericGenerator(name = "notifikasjonDistribusjonIdSeq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "NOTIFIKASJON_DISTRIBUSJON_ID_SEQ")
	})
	@Column(name = "NOTIFIKASJON_DISTRIBUSJON_ID")
	private Integer notifikasjonDistribusjonId;

	@Column(name = "NOTIFIKASJON_ID")
	private String notifikasjonId;

	@Column(name = "K_STATUS")        //Enum?
	private String status;

	@JoinColumn(name = "K_KANAL")
	private String notifikasjonKanal;

	@Column(name = "KONTAKT_INFO")
	private String kontaktInfo;

	@Column(name = "TITTEL")
	private String tittel;

	@Column(name = "TEKST")
	private String tekst;

	@Column(name = "SENDT_DATO")
	private LocalDate notifikasjonSendtDato;

	@Column(name = "OPPRETTET_AV")
	private String opprettetAv;

	@Column(name = "OPPRETTET_DATO")
	private LocalDate opprettetDato;

	@Column(name = "ENDRET_AV")
	private String endretAv;

	@Column(name = "ENDRET_DATO")
	private LocalDate endretDato;
}
