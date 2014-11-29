package ru.rlisystems.docviewer.dao.jpa;

import ru.rlisystems.docviewer.dao.DocumentDAO;
import ru.rlisystems.docviewer.domain.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@ApplicationScoped
public class DocumentDAOImpl implements DocumentDAO
{
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Document create (Document document)
	{
		entityManager.persist(document);
		return document;
	}

	@Override
	public void delete (Integer documentId)
	{
		Document reference = entityManager.getReference(Document.class, documentId);
		entityManager.remove(reference);
	}

	@Override
	public Document fetch (Integer documentId)
	{
		Document document = entityManager.find(Document.class, documentId);
		return document;
	}

	@Override
	public boolean isEmptyRepository ()
	{
		long count = entityManager.createQuery("SELECT COUNT(d) FROM Document d", Long.class).getSingleResult();
		return count == 0;
	}

	@Override
	public List<Document> fetchAllDocuments ()
	{
		TypedQuery<Document> query = entityManager.createQuery("SELECT d FROM Document d", Document.class);
		List<Document> resultList = query.getResultList();
		return resultList;
	}
}
