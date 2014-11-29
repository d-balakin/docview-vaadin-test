package ru.rlisystems.docviewer.ui;

import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.ui.component.PdfJS;

import javax.annotation.PostConstruct;
import java.io.File;

public class FilePreviewPanel extends Panel
{
	private AbstractOrderedLayout layout;
	private Label noContentLabel;
	private PdfJS pdfJS;

	@PostConstruct
	private void postConstruct ()
	{
		addStyleName(ValoTheme.PANEL_BORDERLESS);
		layout = new VerticalLayout();
		layout.setSizeFull();
		setContent(layout);
		noContentLabel = new Label("Нет содержимого для просмотра");
		noContentLabel.setSizeUndefined();

		setDocument(null);
	}

	public void setDocument (Document document)
	{
		if (document == null) {
			layout.removeAllComponents();
			layout.addComponent(noContentLabel);
			layout.setComponentAlignment(noContentLabel, Alignment.MIDDLE_CENTER);
			pdfJS = null;
		}
		else {
			if (pdfJS == null) {
				pdfJS = new PdfJS();
				pdfJS.setSizeFull();
				layout.removeAllComponents();
				layout.addComponent(pdfJS);
			}
			pdfJS.setDocument(new FileResource(new File(document.getStoredFile())));
		}
	}
}
