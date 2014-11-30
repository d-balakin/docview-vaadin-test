package ru.rlisystems.docviewer.dao.jpa;

import ru.rlisystems.docviewer.dao.TransitiveDocumentDAO;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.domain.TransitiveDocument;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TransitiveDocumentDAOImpl implements TransitiveDocumentDAO
{
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public TransitiveDocument fetch (Integer originalDocumentId, String transitiveMimeType)
	{
		Document originalDocumentReference = entityManager.getReference(Document.class, originalDocumentId);
		TypedQuery<TransitiveDocument> query = entityManager.createQuery(
				"SELECT d FROM TransitiveDocument d WHERE d.originalDocument = :orig AND d.mimeType = :mime",
					TransitiveDocument.class);
		query.setParameter("orig", originalDocumentReference);
		query.setParameter("mime", transitiveMimeType);
		TransitiveDocument singleResult = query.getSingleResult();
		return singleResult;
	}

	@Override
	public Optional<TransitiveDocument> fetchOptional (Integer originalDocumentId, String transitiveMimeType)
	{
		try {
			return Optional.of(fetch(originalDocumentId, transitiveMimeType));
		}
		catch (NoResultException ex) {
			return Optional.empty();
		}
	}

	@Override
	public List<TransitiveDocument> fetch (Integer originalDocumentId)
	{
		Document reference = entityManager.getReference(Document.class, originalDocumentId);
		TypedQuery<TransitiveDocument> typedQuery = entityManager.createQuery(
					"SELECT d FROM TransitiveDocument d WHERE d.originalDocument = :orig", TransitiveDocument.class);
		typedQuery.setParameter("orig", reference);
		List<TransitiveDocument> resultList = typedQuery.getResultList();
		return resultList;
	}

	@Override
	public int deleteAll (Integer originalDocumentId)
	{
		Document originalDocument = entityManager.getReference(Document.class, originalDocumentId);
		Query query = entityManager.createQuery("DELETE FROM TransitiveDocument d WHERE d.originalDocument = :orig");
		query.setParameter("orig", originalDocument);
		int count = query.executeUpdate();
		return count;
	}

	@Override
	public TransitiveDocument create (TransitiveDocument transitiveDocument)
	{
		entityManager.persist(transitiveDocument);
		return transitiveDocument;
	}
}
