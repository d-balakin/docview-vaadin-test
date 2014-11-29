package ru.rlisystems.docviewer.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table (name = "TRANSITIVE_DOCUMENTS")
public class TransitiveDocument
{
	@Id
	@Getter @Setter
	@Column (name = "ID")
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

	@NotNull
	@Getter @Setter
	@Column (name = "LAST_ACCESS")
	private Date lastAccess;
}
