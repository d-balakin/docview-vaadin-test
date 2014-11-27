package ru.rlisystems.docviewer.service;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import lombok.Data;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue.NULL;

@ApplicationScoped
public class ContentTypeResolverService
{
	private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\.([^.]+)$");

	@Inject	@ConfigurationValue (name = "ru.rlisystems.docviewer.magic.path", defaultValue = NULL)
	private String magicPath;

	@Inject	@ConfigurationValue (name = "ru.rlisystems.docviewer.magic.resolve_by_ext", defaultValue = "true")
	private boolean resolveByExtension;

	private ContentInfoUtil contentInfoUtil;

	@PostConstruct
	private void postConstruct()
	{
		try {
			contentInfoUtil = magicPath == null ? new ContentInfoUtil() : new ContentInfoUtil(magicPath);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public ContentType resolve (File file) throws IOException
	{
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			return resolve(fileInputStream, file.getName());
		}
	}

	public ContentType resolve (InputStream inputStream, String fileName) throws IOException
	{
		ContentInfo match = contentInfoUtil.findMatch(inputStream);
		if (match != null) {
			return new ContentType(match.getName(), match.getMessage(), match.getMimeType());
		}
		if (resolveByExtension && fileName != null && fileName.isEmpty()) {
			Matcher matcher = EXTENSION_PATTERN.matcher(fileName);
			if (matcher.find()) {
				String extension = matcher.group(1);
				for (com.j256.simplemagic.ContentType contentType : com.j256.simplemagic.ContentType.values()) {
					for (String ext : contentType.getFileExtensions()) {
						if (extension.equals(ext)) {
							return new ContentType(null, null, contentType.getMimeType());
						}
					}
				}
			}
		}
		return null;
	}

	public static @Data	class ContentType
	{
		private final String name;
		private final String description;
		private final String mimeType;
	}
}
