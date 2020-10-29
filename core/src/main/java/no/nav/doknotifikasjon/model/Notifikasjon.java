package no.nav.doknotifikasjon.model;

import lombok.*;
import no.nav.doknotifikasjon.kodeverk.MottakerIdType;
import no.nav.doknotifikasjon.kodeverk.Status;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "T_NOTIFIKASJON")
public class Notifikasjon implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifikasjonIdSeq")
	@GenericGenerator(name = "notifikasjonIdSeq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "NOTIFIKASJON_ID_SEQ")
	})
	@Column(name = "ID")
	private Integer id;

	@Column(name = "BESTILLINGS_ID", length = 40)
	private String bestillingsId;

	@Column(name = "BESTILLER_ID", length = 40)
	private String bestillerId;

	@Column(name = "MOTTAKER_ID", length = 40)
	private String mottakerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_MOTTAKER_ID_TYPE", length = 20)
	private MottakerIdType mottakerIdType;

	@Enumerated(EnumType.STRING)
	@Column(name = "K_STATUS", length = 20)
	private Status status;

	@Column(name = "ANTALL_RENOTIFIKASJONER")
	private Integer antallRenotifikasjoner;

	@Column(name = "RENOTIFIKASJON_INTERVALL")
	private Integer renotifikasjonIntervall;

	@Column(name = "NESTE_RENOTIFIKASJON_DATO")
	private LocalDate nesteRenotifikasjonDato;

	@Column(name = "PREFERERTE_KANALER", length = 20)
	private String prefererteKanaler;

	@Column(name = "OPPRETTET_AV", length = 40)
	private String opprettetAv;

	@Column(name = "OPPRETTET_DATO")
	private LocalDateTime opprettetDato;

	@Column(name = "ENDRET_AV", length = 40)
	private String endretAv;

	@Column(name = "ENDRET_DATO")
	private LocalDateTime endretDato;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@OneToMany(mappedBy = "notifikasjon", cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY)
	private Set<NotifikasjonDistribusjon> notifikasjonDistribusjon = new HashSet<>();
}
