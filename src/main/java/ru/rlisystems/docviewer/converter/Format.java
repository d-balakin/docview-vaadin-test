package ru.rlisystems.docviewer.converter;

import java.util.*;

public enum Format
{
	MICROSOFT_WORD("application/msword", "doc"),
	MICROSOFT_WORD_OOXML("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
	MICROSOFT_EXCEL("application/vnd.ms-excel", "xls"),
	MICROSOFT_EXCEL_OOXML("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
	OPEN_DOCUMENT_TEXT("application/vnd.oasis.opendocument.text", "odt"),
	OPEN_DOCUMENT_SPREADSHEET("application/vnd.oasis.opendocument.spreadsheet", "ods"),
	PORTABLE_DOCUMENT_FORMAT("application/pdf", "pdf"),
	TEXT_PLAIN("text/plain", "txt");

	public final String mimeType;
	public final List<String> fileExtensions;

	private Format (String mimeType, String... fileExtensions)
	{
		this.mimeType = mimeType;
		this.fileExtensions = Collections.unmodifiableList(Arrays.asList(fileExtensions));
	}

	public static Format valueOfMimeType (String mimeType)
	{
		return Arrays.asList(values()).stream()
				.filter(f -> f.mimeType.equals(mimeType)).findFirst().orElse(null);
	}

	public static Format valueOfFileExtension (String fileExtension)
	{
		return Arrays.asList(values()).stream()
				.filter(f -> f.fileExtensions.contains(fileExtension)).findFirst().orElse(null);
	}

	public static FormatMatrixBuilder matrixBuilder ()
	{
		return new FormatMatrixBuilder();
	}

	public static boolean isApplicable(Map<Format, List<Format>> matrixMap, Format from, Format to)
	{
		List<Format> formats = matrixMap.get(from);
		if (formats != null && formats.contains(to)) {
			return true;
		}
		return false;
	}

	public static boolean isApplicable(Map<Format, List<Format>> matrixMap, String fromMimeType, String toMimeType)
	{
		Format from = Format.valueOfMimeType(fromMimeType);
		Format to = Format.valueOfMimeType(toMimeType);
		return from != null && to != null && isApplicable(matrixMap, from, to);
	}

	public static class FormatMatrixBuilder
	{
		private Format from;
		private Map<Format, List<Format>> map = new HashMap<>();
		private FormatMatrixBuilder ()
		{ }

		public FormatMatrixBuilder from (Format format)
		{
			this.from = format;
			return this;
		}

		public FormatMatrixBuilder to (Format ... toFormats)
		{
			if (from == null) {
				throw new IllegalStateException();
			}
			List<Format> toList = map.get(from);
			if (toList == null) {
				toList = new ArrayList<>();
				map.put(from, toList);
			}
			for (Format to : toFormats) {
				toList.add(to);
			}
			return this;
		}

		public Map<Format, List<Format>> build ()
		{
			Map<Format, List<Format>> newMap = new HashMap<>();
			for (Map.Entry<Format, List<Format>> entry : map.entrySet()) {
				List<Format> newList = Collections.unmodifiableList(new ArrayList<>(entry.getValue()));
				newMap.put(entry.getKey(), newList);
			}
			return Collections.unmodifiableMap(newMap);
		}
	}
}
