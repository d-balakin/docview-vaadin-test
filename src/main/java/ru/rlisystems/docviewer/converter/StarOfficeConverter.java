package ru.rlisystems.docviewer.converter;

import lombok.extern.java.Log;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static ru.rlisystems.docviewer.converter.Format.*;

@Log
@Singleton
public class StarOfficeConverter implements FormatConverter
{
	private static final Map<Format, List<Format>> AVAILABLE_CONVERSIONS = Format.matrixBuilder()
			.from(MICROSOFT_WORD).to(MICROSOFT_EXCEL_OOXML, OPEN_DOCUMENT_TEXT, PORTABLE_DOCUMENT_FORMAT)
			.from(MICROSOFT_WORD_OOXML).to(MICROSOFT_WORD, OPEN_DOCUMENT_TEXT, PORTABLE_DOCUMENT_FORMAT)
			.from(MICROSOFT_EXCEL).to(MICROSOFT_EXCEL_OOXML, OPEN_DOCUMENT_SPREADSHEET, PORTABLE_DOCUMENT_FORMAT)
			.from(MICROSOFT_EXCEL_OOXML).to(MICROSOFT_EXCEL, OPEN_DOCUMENT_SPREADSHEET, PORTABLE_DOCUMENT_FORMAT)
			.from(OPEN_DOCUMENT_TEXT).to(MICROSOFT_WORD, MICROSOFT_EXCEL_OOXML, PORTABLE_DOCUMENT_FORMAT)
			.from(OPEN_DOCUMENT_SPREADSHEET).to(MICROSOFT_EXCEL, MICROSOFT_EXCEL_OOXML, PORTABLE_DOCUMENT_FORMAT)
			.from(TEXT_PLAIN).to(PORTABLE_DOCUMENT_FORMAT)
		.build();

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.soffice_exec", defaultValue = "soffice")
	private Instance<String> starOfficePathParam;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.soffice_timeout_ms", defaultValue = "60000")
	private Instance<Integer> processTimeoutMs;

	private String checkedStarOfficePath;
	private boolean starOfficeAvailable;

	@Override
	public boolean isApplicable (Format fromFormat, Format toFormat)
	{
		return Format.isApplicable(AVAILABLE_CONVERSIONS, fromFormat, toFormat) && getStarOfficePath() != null;
	}

	@Override
	public ConversationTask makeConversationTask (Format fromFormat, Format toFormat,
												  File from, File to)
	{
		if (!isApplicable(fromFormat, toFormat)) {
			throw new IllegalArgumentException();
		}
		String toFormatExtension = toFormat.fileExtensions.get(0);

		return new AbstractConversationTask()
		{
			@Override
			public void run ()
			{
				setState(ConversationTaskEvent.State.IN_PROCESS);
				try {
					File tempDirectory = Files.createTempDirectory("docviewer-so-").toFile();
					try {
						String[] command = { getStarOfficePath(), "--headless", "--convert-to", toFormatExtension,
								"--outdir", tempDirectory.getPath(), from.getPath() };
						Process process = new ProcessBuilder(command).start();
						log.finest("Запуск процесса конвертации " + Arrays.toString(command));
						if (process.waitFor(processTimeoutMs.get(), TimeUnit.MILLISECONDS))
						{
							File[] files = tempDirectory.listFiles();
							if (process.exitValue() == 0 && files != null && files.length == 1) {
								Files.move(files[0].toPath(), to.toPath());
								setState(ConversationTaskEvent.State.COMPLETED);
								log.finest("Конвертация завершена " + Arrays.toString(command));
							}
							else {
								setState(ConversationTaskEvent.State.FAILURE);
								log.severe("Ошибка конвертации '" + readProcessOutput(process, true) + "' (" +
																					Arrays.toString(command) + ")");
							}
						}
						else {
							log.severe("Таймаут конвертации");
							setState(ConversationTaskEvent.State.FAILURE);
							process.destroyForcibly().waitFor();
						}
					}
					finally {
						removeDirectoryWithContent(tempDirectory.toPath());
					}
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Ошибка конвертации документа", ex);
					if (getState() != ConversationTaskEvent.State.COMPLETED) {
						setState(ConversationTaskEvent.State.FAILURE);
					}
				}
			}
		};
	}

	private static void removeDirectoryWithContent (Path directory) throws IOException
	{
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
			{
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private synchronized String getStarOfficePath ()
	{
		String starOfficePath = this.starOfficePathParam.get();
		if (!Objects.equals(starOfficePath, checkedStarOfficePath)) {
			checkedStarOfficePath = starOfficePath;
			starOfficeAvailable = false;
			try {
				log.finest("Определение версии soffice");
				Process process = new ProcessBuilder(starOfficePath, "--version").start();
				if (process.waitFor(processTimeoutMs.get(), TimeUnit.MILLISECONDS)) {
					if (process.exitValue() == 0) {
						String output = readProcessOutput(process, false);
						starOfficeAvailable = true;
						log.info("Используется " + output.replaceAll("[\r\n]", ""));
					}
				}
				else {
					process.destroyForcibly().waitFor();
				}
				if (!starOfficeAvailable) {
					log.finest("soffice недоступен");
				}
			}
			catch (Exception ex) {
				log.log(Level.FINEST, "", ex);
			}
		}
		return starOfficeAvailable ? checkedStarOfficePath : null;
	}

	private static String readProcessOutput (Process process, boolean errorStream) throws IOException
	{
		StringBuilder stringBuilder = new StringBuilder();
		try (InputStream is = errorStream ? process.getErrorStream() : process.getInputStream();
			 InputStreamReader isr = new InputStreamReader(is))
		{
			char[] buf = new char[1024];
			for (int c; (c = isr.read(buf)) != -1;) {
				stringBuilder.append(buf, 0, c);
			}
		}
		return stringBuilder.toString();
	}

	@Override
	public String getConverterClass ()
	{
		return "soffice";
	}
}
