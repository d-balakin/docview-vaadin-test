package ru.rlisystems.docviewer.dao;

import ru.rlisystems.docviewer.domain.TransitiveDocument;

import java.util.List;
import java.util.Optional;

public interface TransitiveDocumentDAO
{
	TransitiveDocument fetch (Integer originalDocumentId, String transitiveMimeType);
	List<TransitiveDocument> fetch (Integer originalDocumentId);
	int deleteAll (Integer originalDocumentId);
	Optional<TransitiveDocument> fetchOptional (Integer originalDocumentId, String transitiveMimeType);
	TransitiveDocument create (TransitiveDocument transitiveDocument);
}
