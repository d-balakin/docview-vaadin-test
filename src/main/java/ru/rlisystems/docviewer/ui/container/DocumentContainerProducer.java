package ru.rlisystems.docviewer.ui.container;

import com.vaadin.cdi.UIScoped;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.domain.event.EntityEvent;
import ru.rlisystems.docviewer.service.DocumentService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.List;
import java.util.WeakHashMap;

@ApplicationScoped
public class DocumentContainerProducer
{
	@Inject
	private DocumentService documentService;

	private WeakHashMap<BeanItemMemoryContainer<Integer, Document>, Void> containers = new WeakHashMap<>();

	@Produces
	public BeanItemContainer<Integer, Document> produce ()
	{
		BeanItemMemoryContainer<Integer, Document> container = new BeanItemMemoryContainer<>(Document.class);
		container.setBeanIdProperty("id");
		container.addAll(documentService.fetchAllDocuments());
		containers.put(container, null);
		return container;
	}

	public synchronized void refresh (@Observes EntityEvent<Document> entityEvent)
	{
		List<Document> documents = documentService.fetchAllDocuments();
		for (BeanItemMemoryContainer<Integer, Document> container : containers.keySet()) {
			container.removeAllItems();
			container.addAll(documents);
		}
	}
}
