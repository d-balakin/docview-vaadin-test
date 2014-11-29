package ru.rlisystems.docviewer.dao;

import ru.rlisystems.docviewer.domain.Document;

import java.util.List;

public interface DocumentDAO
{
	Document fetch (Integer documentId);
	List<Document> fetchAllDocuments ();
	Document create (Document document);
	void delete (Integer documentId);
	boolean isEmptyRepository ();
}
