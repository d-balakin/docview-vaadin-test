package ru.rlisystems.docviewer.service;

import lombok.extern.java.Log;
import ru.rlisystems.docviewer.dao.DocumentDAO;
import ru.rlisystems.docviewer.service.DocumentService.DocumentReceiver;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;

@Log
@Startup
@javax.ejb.Singleton
public class SampleContentService
{
	@Inject
	private DocumentDAO documentDAO;

	@Inject
	private DocumentService documentService;

	@PostConstruct
	private void postConstruct ()
	{
		if (documentDAO.isEmptyRepository()) {
			byte[] buf = new byte[4096];
			for (String file : Arrays.asList("sample.doc", "sample.docx", "sample.odt", "sample.ods", "sample.pdf",
											 "sample.xls", "sample.xlsx"))
			{
				DocumentReceiver documentReceiver = documentService.makeDocumentReceiver(file, null);
				try (InputStream inputStream = DocumentService.class.getResourceAsStream(file);
					 OutputStream outputStream = documentReceiver.getReceiverOutputStream())
				{
					for (int c; (c = inputStream.read(buf)) != -1;) {
						outputStream.write(buf, 0, c);
					}
					documentReceiver.persist();
				}
				catch (IOException ex) {
					documentReceiver.cancel();
					log.log(Level.SEVERE, "", ex);
				}
			}
		}
	}
}
