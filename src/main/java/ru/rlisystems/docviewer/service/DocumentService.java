package ru.rlisystems.docviewer.service;

import lombok.Data;
import lombok.extern.java.Log;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;
import ru.rlisystems.docviewer.TransactionUtils.AfterCompletionSynchronization;
import ru.rlisystems.docviewer.converter.Format;
import ru.rlisystems.docviewer.converter.FormatConverter.ConversationTask;
import ru.rlisystems.docviewer.dao.DocumentDAO;
import ru.rlisystems.docviewer.dao.TransitiveDocumentDAO;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.domain.TransitiveDocument;
import ru.rlisystems.docviewer.domain.event.EntityEvent;
import ru.rlisystems.docviewer.service.ContentTypeResolverService.ContentType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

import static ru.rlisystems.docviewer.converter.FormatConverter.ConversationTaskEvent.State.COMPLETED;
import static ru.rlisystems.docviewer.converter.FormatConverter.ConversationTaskEvent.State.FAILURE;

@Log
@ApplicationScoped
public class DocumentService
{
	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.storage", defaultValue = ConfigurationValue.NULL)
	private Instance<String> storagePath;

	@Inject
	private ContentTypeResolverService contentTypeResolverService;

	@Inject
	private DocumentDAO documentDAO;

	@Inject
	private TransitiveDocumentDAO transitiveDocumentDAO;

	@Inject
	private Event<EntityEvent<Document>> documentEntityEvent;

	@Inject
	private DocumentService documentService;

	@Inject
	private FormatConverterExecutorService formatConverterExecutorService;

	@Inject
	private TransactionManager transactionManager;

	@Transactional
	public Document create (Document document)
	{
		documentDAO.create(document);
		documentEntityEvent.fire(EntityEvent.create(document));
		return document;
	}

	@Transactional
	public void delete (Integer originalDocumentId)
	{
		List<TransitiveDocument> transitiveDocuments = transitiveDocumentDAO.fetch(originalDocumentId);
		transitiveDocumentDAO.deleteAll(originalDocumentId);
		Document document = documentDAO.fetch(originalDocumentId);
		documentDAO.delete(originalDocumentId);
		documentEntityEvent.fire(EntityEvent.create(Document.class));
		try {
			transactionManager.getTransaction().registerSynchronization((AfterCompletionSynchronization) status -> {
				File file = new File(document.getStoredFile());
				if (status == Status.STATUS_COMMITTED) {
					if (!file.delete()) {
						log.warning("Не удалось удалить файл '" + file + "'");
					}
					for (TransitiveDocument transitiveDocument : transitiveDocuments) {
						if (!new File(transitiveDocument.getStoredFile()).delete()) {
							log.warning("Не удалось удалить файл '" + file + "'");
						}
					}
				}
				else {
					log.finest("Файл '" + file + "' не удалён в связи с неудачной транзакцией");
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public DocumentReceiver makeDocumentReceiver (String fileName, String mimeType)
	{
		try {
			File file = makeStorageFile();
			return new DocumentReceiver()
			{
				@Override
				public OutputStream getReceiverOutputStream ()
				{
					try {
						return new FileOutputStream(file);
					}
					catch (FileNotFoundException ex) {
						throw new RuntimeException(ex);
					}
				}

				@Override
				public Document persist ()
				{
					String _mimeType = mimeType;
					if (_mimeType == null || _mimeType.isEmpty()) {
						try (InputStream inputStream = new FileInputStream(file)) {
							ContentType resolve = contentTypeResolverService.resolve(inputStream, fileName);
							if (resolve != null) {
								_mimeType = resolve.getMimeType();
							}
							else {
								_mimeType = "application/octet-stream";
							}
						}
						catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					}
					try {
						Document document = new Document();
						document.setFileName(fileName);
						document.setMimeType(_mimeType);
						document.setFileSize(file.length());
						document.setStoredFile(file.getPath());
						documentService.create(document);
						return document;
					}
					catch (RuntimeException ex) {
						if (!file.delete()) {
							log.warning("Не удалось удалить файл '" + file + "'");
						}
						throw ex;
					}
				}

				@Override
				public void cancel ()
				{
					if (!file.delete()) {
						log.warning("Не удалось удалить файл '" + file + "'");
					}
				}
			};
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public List<Document> fetchAllDocuments ()
	{
		return documentDAO.fetchAllDocuments();
	}

	private static @Data class TransitiveDocumentKey
	{
		private final int originalDocumentId;
		private final Format transitiveFormat;
	}

	private class TransitiveDocumentTaskImpl implements TransitiveDocumentTask, Runnable
	{
		private final List<TransitiveDocumentEventListener> eventListeners =
													Collections.synchronizedList(new ArrayList<>());
		private final TransitiveDocumentKey key;
		private final Document originalDocument;
		private TransitiveDocumentEvent transitiveDocumentEvent;

		private TransitiveDocumentTaskImpl (TransitiveDocumentKey key, Document originalDocument)
		{
			this.key = key;
			this.originalDocument = originalDocument;
		}

		@Override
		public void addTransitiveDocumentEventListener (TransitiveDocumentEventListener listener)
		{
			synchronized (eventListeners) {
				eventListeners.add(listener);
				if (transitiveDocumentEvent != null) {
					listener.onTransitiveDocumentEvent(transitiveDocumentEvent);
				}
			}
		}

		@Override
		public void run ()
		{
			Format fromFormat = Format.valueOfMimeType(originalDocument.getMimeType());
			if (fromFormat == null) {
				done(null);
			}
			else {
				try {
					File transitiveFile = makeStorageFile();
					ConversationTask process = formatConverterExecutorService.process(fromFormat,
							  key.getTransitiveFormat(), new File(originalDocument.getStoredFile()), transitiveFile);
					process.addConversationEventListener(conversationTaskEvent -> {
						if (conversationTaskEvent.getState() == FAILURE) {
							done(null);
						}
						else if (conversationTaskEvent.getState() == COMPLETED) {
							TransitiveDocument newTransitiveDocument = new TransitiveDocument();
							newTransitiveDocument.setMimeType(key.getTransitiveFormat().mimeType);
							newTransitiveDocument.setOriginalDocument(originalDocument);
							newTransitiveDocument.setStoredFile(transitiveFile.getPath());
							try {
								documentService.create(newTransitiveDocument);
								done(newTransitiveDocument);
							}
							catch (PersistenceException ex) {
								Optional<TransitiveDocument> existing =	transitiveDocumentDAO.fetchOptional(
										key.getOriginalDocumentId(), key.getTransitiveFormat().mimeType);
								if (existing.isPresent()) {
									log.finest("Гонка, откат " + existing.get());
									if (!transitiveFile.delete()) {
										log.warning("Не удалось удалить файл '" + transitiveFile + "'");
									}
									done(existing.get());
								}
								else {
									log.log(Level.SEVERE, "", ex);
									done(null);
								}
							}
						}
					});
				}
				catch (Exception ex) {
					done(null);
				}
			}
		}

		private void done (TransitiveDocument transitiveDocument)
		{
			synchronized (eventListeners) {
				transitiveDocumentEvent = new TransitiveDocumentEvent(transitiveDocument);
				for (TransitiveDocumentEventListener eventListener : eventListeners) {
					eventListener.onTransitiveDocumentEvent(transitiveDocumentEvent);
				}
			}
			TRANSITIVE_TASK_MAP.remove(key);
		}
	}

	private static final Map<TransitiveDocumentKey, TransitiveDocumentTaskImpl> TRANSITIVE_TASK_MAP =
																Collections.synchronizedMap(new HashMap<>());

	public TransitiveDocumentTask makeTransitiveDocument (Integer originalDocumentId, Format transitiveFormat)
	{
		Document originalDocument = documentDAO.fetch(originalDocumentId);
		TransitiveDocumentKey transitiveDocumentKey = new TransitiveDocumentKey(originalDocumentId, transitiveFormat);
		TransitiveDocumentTaskImpl transitiveDocumentTask;
		boolean fresh = false;
		synchronized (TRANSITIVE_TASK_MAP) {
			transitiveDocumentTask = TRANSITIVE_TASK_MAP.get(transitiveDocumentKey);
			if (transitiveDocumentTask == null) {
				transitiveDocumentTask = new TransitiveDocumentTaskImpl(transitiveDocumentKey, originalDocument);
				TRANSITIVE_TASK_MAP.put(transitiveDocumentKey, transitiveDocumentTask);
				fresh = true;
			}
		}
		if (fresh) {
			transitiveDocumentTask.run();
		}
		return transitiveDocumentTask;
	}

	public Optional<TransitiveDocument> fetchTransitiveDocument (Integer originalDocumentId, Format transitiveFormat)
	{
		return transitiveDocumentDAO.fetchOptional(originalDocumentId, transitiveFormat.mimeType);
	}

	@Transactional
	public TransitiveDocument create (TransitiveDocument transitiveDocument)
	{
		return transitiveDocumentDAO.create(transitiveDocument);
	}

	private File makeStorageFile () throws IOException
	{
		String storagePath = this.storagePath.get();
		if (storagePath == null) {
			return File.createTempFile("doc-", ".bin");
		}
		else {
			return File.createTempFile("doc-", ".bin", new File(storagePath));
		}
	}

	public static interface DocumentReceiver
	{
		OutputStream getReceiverOutputStream ();
		Document persist ();
		void cancel ();
	}

	public static interface TransitiveDocumentTask
	{
		void addTransitiveDocumentEventListener (TransitiveDocumentEventListener listener);
	}

	public static interface TransitiveDocumentEventListener
	{
		void onTransitiveDocumentEvent (TransitiveDocumentEvent event);
	}

	public static @Data class TransitiveDocumentEvent
	{
		private final TransitiveDocument transitiveDocument;
	}
}
