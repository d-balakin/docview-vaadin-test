package ru.rlisystems.docviewer.ui;

import com.vaadin.event.Action;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import lombok.extern.java.Log;
import org.vaadin.dialogs.ConfirmDialog;
import ru.rlisystems.docviewer.converter.Format;
import ru.rlisystems.docviewer.domain.Document;
import ru.rlisystems.docviewer.service.DocumentService;
import ru.rlisystems.docviewer.service.DocumentService.DocumentReceiver;
import ru.rlisystems.docviewer.ui.container.BeanItemContainer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class Workspace extends Panel
{
	private static final String[] UNITS = new String[] { "B", "KiB", "MiB", "GiB", "TiB" };

	@Inject
	private BeanItemContainer<Integer, Document> documentContainer;

	@Inject
	private DocumentService documentService;

	@Inject
	private FilePreviewPanel filePreviewPanel;

	@PostConstruct
	private void postConstruct ()
	{
		setCaption("Просмотр документов");
		addStyleName(MyTheme.PANEL_SHADOW);

		VerticalLayout panelLayout = new VerticalLayout();
		panelLayout.setSizeFull();
		setContent(panelLayout);

		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		splitPanel.addStyleName(ValoTheme.SPLITPANEL_LARGE);
		panelLayout.addComponent(splitPanel);
		panelLayout.setExpandRatio(splitPanel, 1);
		splitPanel.setSizeFull();

		VerticalLayout fileTableLayout = new VerticalLayout();
		splitPanel.setFirstComponent(fileTableLayout);
		fileTableLayout.setSizeFull();

		Table fileTable = new Table();
		fileTable.setPageLength(0);
		fileTableLayout.addComponent(fileTable);
		fileTableLayout.setExpandRatio(fileTable, 1);
		fileTable.addStyleName(ValoTheme.TABLE_BORDERLESS);
		fileTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
		fileTable.setSizeFull();
		fileTable.setSelectable(true);
		fileTable.setContainerDataSource(documentContainer);
		fileTable.addGeneratedColumn("_typeIcon", (source, itemId, columnId) -> {
			String mimeType = documentContainer.getItem(itemId).getBean().getMimeType();
			FontAwesome icon = mimeIcons(mimeType);
			Label label = new Label(icon.getHtml());
			label.setContentMode(ContentMode.HTML);
			return label;
		});
		fileTable.addGeneratedColumn("_fileSize", (source, itemId, columnId) -> {
			long fileSize = documentContainer.getItem(itemId).getBean().getFileSize();
			if (fileSize <= 0) {
				return "0";
			}
			int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
			return new DecimalFormat("#,##0.#").format(
					fileSize / Math.pow(1024, digitGroups)) + " " + UNITS[digitGroups];

		});
		fileTable.setVisibleColumns("_typeIcon", "fileName", "_fileSize");
		fileTable.setColumnHeader("_typeIcon", "");
		fileTable.setColumnHeader("fileName", "Имя файла");
		fileTable.setColumnHeader("_fileSize", "Размер");
		fileTable.setColumnWidth("_typeIcon", 30);
		fileTable.setColumnWidth("_fileSize", 100);
		fileTable.setSortContainerPropertyId("fileName");
		fileTable.setSortAscending(true);

		Map<String, DocumentReceiver> receiverMap = new HashMap<>();
		Upload upload = new Upload();
		upload.addStyleName(MyTheme.UPLOAD_BUTTON);
		upload.setButtonCaption("Загрузить");
		upload.setImmediate(true);

		fileTableLayout.addComponent(upload);
		fileTableLayout.setComponentAlignment(upload, Alignment.MIDDLE_CENTER);

		filePreviewPanel.setSizeFull();
		splitPanel.setSecondComponent(filePreviewPanel);

		fileTable.addActionHandler(new Action.Handler()
		{
			Action deleteAction = new Action("Удалить", FontAwesome.TIMES);
			Action[] actions = { deleteAction };

			@Override
			public Action[] getActions (Object target, Object sender)
			{
				return actions;
			}

			@Override
			@SuppressWarnings ("deprecation")
			public void handleAction (Action action, Object sender, Object target)
			{
				if (target == null) {
					return;
				}
				Document document = documentContainer.getItem(target).getBean();
				if (action == deleteAction) {
					ConfirmDialog.show(getUI(), "Подтверждение",
									   "Вы действительно хотите удалить файл '" + document.getFileName() + "'",
									   "Да", "Отмена", c ->
					{
						if (c.isConfirmed()) {
							documentService.delete((Integer) target);
						}
					});
				}
			}
		});

		fileTable.addValueChangeListener(event -> {
			Object itemId = event.getProperty().getValue();
			if (itemId == null) {
				filePreviewPanel.setDocument(null);
			}
			else {
				Document bean = documentContainer.getItem(event.getProperty().getValue()).getBean();
				filePreviewPanel.setDocument(bean);
			}
		});

		upload.setReceiver((fileName, mimeType) -> {
			if (receiverMap.containsKey(fileName)) {
				throw new RuntimeException();
			}
			DocumentReceiver documentReceiver = documentService.makeDocumentReceiver(fileName, mimeType);
			receiverMap.put(fileName, documentReceiver);
			return documentReceiver.getReceiverOutputStream();
		});
		upload.addSucceededListener(event -> {
			DocumentReceiver documentReceiver = receiverMap.remove(event.getFilename());
			try {
				documentReceiver.persist();
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "", ex);
			}
		});
		upload.addFailedListener(event -> {
			DocumentReceiver documentReceiver = receiverMap.remove(event.getFilename());
			documentReceiver.cancel();
		});
	}

	private static FontAwesome mimeIcons (String mimeType)
	{
		if (mimeType == null) {
			return FontAwesome.FILE_O;
		}
		else if (mimeType.equals(Format.MICROSOFT_EXCEL.mimeType)
			  || mimeType.equals(Format.MICROSOFT_EXCEL_OOXML.mimeType)
			  || mimeType.equals(Format.OPEN_DOCUMENT_SPREADSHEET.mimeType))
		{
			return FontAwesome.FILE_EXCEL_O;
		}
		else if (mimeType.equals(Format.MICROSOFT_WORD.mimeType)
			  || mimeType.equals(Format.MICROSOFT_WORD_OOXML.mimeType)
			  || mimeType.equals(Format.OPEN_DOCUMENT_TEXT.mimeType))
		{
			return FontAwesome.FILE_WORD_O;
		}
		else if (mimeType.equals(Format.PORTABLE_DOCUMENT_FORMAT.mimeType)) {
			return FontAwesome.FILE_PDF_O;
		}
		else if (mimeType.equals(Format.TEXT_PLAIN.mimeType)) {
			return FontAwesome.FILE_TEXT_O;
		}
		else {
			return FontAwesome.FILE_O;
		}
	}
}
