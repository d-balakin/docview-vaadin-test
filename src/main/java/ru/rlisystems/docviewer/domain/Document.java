package ru.rlisystems.docviewer.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@ToString
@Table (name = "DOCUMENTS")
public class Document
{
	@Id
	@Getter	@Setter
	@Column (name = "ID")
	@GeneratedValue (strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotNull
	@Getter @Setter
	@Column (name = "FILE_NAME")
	private String fileName;

	@NotNull
	@Getter @Setter
	@Column (name = "FILE_SIZE")
	private long fileSize;

	@NotNull
	@Getter @Setter
	@Column (name = "STORED_FILE")
	private String storedFile;

	@NotNull
	@Getter @Setter
	@Column (name = "MIME_TYPE")
	private String mimeType;
}
