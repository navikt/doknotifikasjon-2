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
@Table(name = "T_NOTIFIKASJON")
public class Notifikasjon implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifikasjonIdSeq")
	@GenericGenerator(name = "notifikasjonIdSeq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "NOTIFIKASJON_ID_SEQ")
	})
	@Column(name = "NOTIFIKASJON_ID")
	private Integer notifikasjonId;

	@Column(name = "BESTILLING_ID")
	private String bestillingId;

	@JoinColumn(name = "BESTILLER_ID")
	private String bestillerId;

	@Column(name = "MOTTAKER_ID")
	private String mottakerId;

	@Column(name = "K_MOTTAKER_ID_TYPE")    //Enum?
	private String mottakerIdType;

	@Column(name = "K_STATUS")        //Enum?
	private String status;

	@Column(name = "ANTALL_RENOTIFIKASJONER")
	private Integer antallRenotifikasjoner;

	@Column(name = "RENOTIFIKASJON_INTERVALL")
	private Integer renotifikasjonIntervall;

	@Column(name = "NESTE_RENOTIFIKASJON_DATO")
	private LocalDate nesteRenotifikasjonDato;

	@Column(name = "PREFERERTE_KANALER")
	private String prefererteKanaler;

	@Column(name = "OPPRETTET_AV")
	private String opprettetAv;

	@Column(name = "OPPRETTET_DATO")
	private LocalDate opprettetDato;

	@Column(name = "ENDRET_AV")
	private String endretAv;

	@Column(name = "ENDRET_DATO")
	private LocalDate endretDato;

}
