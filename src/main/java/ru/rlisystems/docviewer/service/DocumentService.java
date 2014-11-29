package ru.rlisystems.docviewer.service;

import lombok.extern.java.Log;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;
import ru.rlisystems.docviewer.TransactionUtils.AfterCompletionSynchronization;
import ru.rlisystems.docviewer.dao.DocumentDAO;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.domain.event.EntityEvent;
import ru.rlisystems.docviewer.service.ContentTypeResolverService.ContentType;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

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
	private Event<EntityEvent<Document>> documentEntityEvent;

	@Inject
	private DocumentService documentService;

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
	public void delete (Integer documentId)
	{
		Document document = documentDAO.fetch(documentId);
		documentDAO.delete(documentId);
		documentEntityEvent.fire(EntityEvent.create(Document.class));
		try {
			transactionManager.getTransaction().registerSynchronization((AfterCompletionSynchronization) status -> {
				File file = new File(document.getStoredFile());
				if (status == Status.STATUS_COMMITTED) {
					if (!file.delete()) {
						log.warning("Не удалось удалить файл '" + file + "'");
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
}
