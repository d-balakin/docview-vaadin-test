package ru.rlisystems.docviewer.ui;

import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import ru.rlisystems.docviewer.converter.Format;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.domain.TransitiveDocument;
import ru.rlisystems.docviewer.service.DocumentService;
import ru.rlisystems.docviewer.service.DocumentService.TransitiveDocumentTask;
import ru.rlisystems.docviewer.ui.component.PdfJS;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Optional;

public class FilePreviewPanel extends Panel
{
	@Inject
	private DocumentService documentService;

	private AbstractOrderedLayout layout;
	private Label noContentLabel;
	private Label documentProcessingLabel;
	private Label documentNotAvailableLabel;
	private PdfJS pdfJS;

	@PostConstruct
	private void postConstruct ()
	{
		addStyleName(ValoTheme.PANEL_BORDERLESS);
		layout = new VerticalLayout();
		layout.setSizeFull();
		setContent(layout);

		noContentLabel = new Label("Нет содержимого для просмотра");

		documentProcessingLabel = new Label(
				"<i class=\"fa fa-spinner fa-spin\"></i>Идёт подготовка документа...", ContentMode.HTML);

		documentNotAvailableLabel = new Label("Документ недоступен для просмотра");

		pdfJS = new PdfJS();
		pdfJS.setSizeFull();

		setDocument(null);
	}

	private void setLayoutLabel (Label label)
	{
		if (!label.isAttached()) {
			layout.removeAllComponents();
			label.setSizeUndefined();
			layout.addComponent(label);
			layout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		}
	}

	private void setDocumentFile (File file)
	{
		if (!pdfJS.isAttached()) {
			layout.removeAllComponents();
			layout.addComponent(pdfJS);
		}
		pdfJS.setDocument(new FileResource(file));
	}

	public void setDocument (Document document)
	{
		if (document == null) {
			setLayoutLabel(noContentLabel);
		}
		else {
			if (Format.PORTABLE_DOCUMENT_FORMAT.mimeType.equals(document.getMimeType())) {
				setDocumentFile(new File(document.getStoredFile()));
			}
			else {
				Optional<TransitiveDocument> transitiveDocument =
						documentService.fetchTransitiveDocument(document.getId(), Format.PORTABLE_DOCUMENT_FORMAT);
				if (transitiveDocument.isPresent()) {
					setDocumentFile(new File(transitiveDocument.get().getStoredFile()));
				}
				else {
					UI ui = UI.getCurrent();
					TransitiveDocumentTask transitiveDocumentTask = documentService.makeTransitiveDocument(
																document.getId(), Format.PORTABLE_DOCUMENT_FORMAT);
					transitiveDocumentTask.addTransitiveDocumentEventListener(event -> ui.access(() -> {
						TransitiveDocument newTransitiveDocument = event.getTransitiveDocument();
						if (newTransitiveDocument != null) {
							setDocumentFile(new File(newTransitiveDocument.getStoredFile()));
						}
						else {
							setLayoutLabel(documentNotAvailableLabel);
						}
					}));
					setLayoutLabel(documentProcessingLabel);
				}
			}
		}
	}
}
