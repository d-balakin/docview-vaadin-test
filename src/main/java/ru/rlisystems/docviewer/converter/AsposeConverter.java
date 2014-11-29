package ru.rlisystems.docviewer.converter;

import lombok.extern.java.Log;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static ru.rlisystems.docviewer.converter.Format.*;

@Log
public class AsposeConverter implements FormatConverter
{
	private static final Map<Format, List<Format>> AVAILABLE_WORDS_CONVERSIONS = Format.matrixBuilder()
			.from(MICROSOFT_WORD).to(PORTABLE_DOCUMENT_FORMAT)
			.from(MICROSOFT_WORD_OOXML).to(PORTABLE_DOCUMENT_FORMAT)
		.build();

	private static final Map<Format, List<Format>> AVAILABLE_CELLS_CONVERSIONS = Format.matrixBuilder()
			.from(MICROSOFT_EXCEL).to(PORTABLE_DOCUMENT_FORMAT)
			.from(MICROSOFT_EXCEL_OOXML).to(PORTABLE_DOCUMENT_FORMAT)
		.build();

	private Class<?> workbookClass;
	private Class<?> documentClass;

	@PostConstruct
	private void postConstruct ()
	{
		try {
			workbookClass = Class.forName("com.aspose.cells.Workbook");
		}
		catch (ClassNotFoundException ex) {
			log.finest("Asopse.Cells недоступен");
		}
		try {
			documentClass = Class.forName("com.aspose.words.Document");
		}
		catch (ClassNotFoundException ex) {
			log.finest("Asopse.Words недоступен");
		}
	}

	@Override
	public boolean isApplicable (Format fromFormat, Format toFormat)
	{
		if (workbookClass != null && Format.isApplicable(AVAILABLE_CELLS_CONVERSIONS, fromFormat, toFormat)) {
			return true;
		}
		if (documentClass != null && Format.isApplicable(AVAILABLE_WORDS_CONVERSIONS, fromFormat, toFormat)) {
			return true;
		}
		return false;
	}

	@Override
	public ConversationTask makeConversationTask (Format fromFormat, Format toFormat, File from, File to)
	{
		if (!isApplicable(fromFormat, toFormat)) {
			throw new IllegalArgumentException();
		}
		return new AbstractConversationTask()
		{
			@Override
			public void run ()
			{
				setState(ConversationTaskEvent.State.IN_PROCESS);
				try {
					if (AVAILABLE_CELLS_CONVERSIONS.containsKey(fromFormat)) {
						Object workbook = workbookClass.getConstructor(String.class).newInstance(from.getPath());
						workbook.getClass().getMethod("save", String.class, int.class)
														.invoke(workbook, to.getPath(), 13);
					}
					else if (AVAILABLE_WORDS_CONVERSIONS.containsKey(fromFormat)) {
						Object document = documentClass.getConstructor(String.class).newInstance(from.getPath());
						document.getClass().getMethod("save", String.class, int.class)
														.invoke(document, to.getPath(), 40);
					}
					else {
						throw new RuntimeException();
					}
					setState(ConversationTaskEvent.State.COMPLETED);
					log.finest("Конвертация завершена");
				}
				catch (Exception ex) {
					setState(ConversationTaskEvent.State.FAILURE);
					log.log(Level.SEVERE, "Ошибка конверации", ex);
				}
			}
		};
	}

	@Override
	public String getConverterClass ()
	{
		return "aspose";
	}
}
