package ru.rlisystems.docviewer.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table (name = "TRANSITIVE_DOCUMENTS", uniqueConstraints =
										@UniqueConstraint(columnNames = { "DOCUMENT_ID", "MIME_TYPE" }))
@Entity
@ToString
public class TransitiveDocument
{
	@Id
	@Getter @Setter
	@Column (name = "ID")
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotNull
	@ManyToOne
	@Getter @Setter
	@JoinColumn (name = "DOCUMENT_ID")
	private Document originalDocument;

	@NotNull
	@Getter @Setter
	@Column (name = "STORED_FILE")
	private String storedFile;

	@NotNull
	@Getter @Setter
	@Column (name = "MIME_TYPE")
	private String mimeType;
}
