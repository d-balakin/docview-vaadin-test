package ru.rlisystems.docviewer.service;

import lombok.extern.java.Log;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;
import ru.rlisystems.docviewer.converter.Format;
import ru.rlisystems.docviewer.converter.FormatConverter;
import ru.rlisystems.docviewer.converter.FormatConverter.ConversationTask;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Log
@ApplicationScoped
public class FormatConverterExecutorService
{
	@Inject
	private Instance<FormatConverter> formatConverterInstance;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.max_pool_sz", defaultValue = "-1")
	private int maximumPoolSize;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.pool_keepalive_ms", defaultValue = "60000")
	private int poolKeepAliveMs;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.pool_queue_max", defaultValue = "100")
	private int poolQueueCapacity;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.preferred_class", defaultValue = "soffice")
	private Instance<String> preferredConverterClass;

	@Resource
	private ManagedThreadFactory managedThreadFactory;

	private BlockingQueue<Runnable> queue;
	private ExecutorService executorService;

	@PostConstruct
	private void postConstruct ()
	{
		queue = new LinkedBlockingQueue<>(poolQueueCapacity);
		int maximumPoolSize = this.maximumPoolSize;
		if (maximumPoolSize < 1) {
			maximumPoolSize = Runtime.getRuntime().availableProcessors();
		}
		executorService = new ThreadPoolExecutor(0, maximumPoolSize, poolKeepAliveMs, TimeUnit.MILLISECONDS,
												 queue, managedThreadFactory);
		log.fine(String.format("Инициализирован пул сервиса преобразования документов, макс. количество потоков: %d, " +
							   "таймаут %dмс, макс. размер очереди: %d, предпочитаемый класс преобразователя: %s",
				 				maximumPoolSize, poolKeepAliveMs, poolQueueCapacity, preferredConverterClass.get()));
	}

	public ConversationTask process (Format fromFormat, Format toFormat, File from, File to)
	{
		List<FormatConverter> converters = new ArrayList<>();
		for (FormatConverter formatConverter : formatConverterInstance) {
			if (formatConverter.isApplicable(fromFormat, toFormat)) {
				converters.add(formatConverter);
			}
		}
		if (converters.isEmpty()) {
			throw new IllegalArgumentException("Преобразование " + from + " => " + to + " не поддерживается");
		}
		FormatConverter formatConverter = converters.get(0);
		String preferredConverterClass = this.preferredConverterClass.get();
		for (FormatConverter converter : converters) {
			if (Objects.equals(preferredConverterClass, converter.getConverterClass())) {
				formatConverter = converter;
			}
		}
		ConversationTask conversationTask = formatConverter.makeConversationTask(fromFormat, toFormat, from, to);
		executorService.submit(conversationTask);
		log.finest(String.format("Отправлена задача, размер очереди: %d/%d, преобразование: %s => %s," +
								 " файлы: %s => %s, конвертор: %s",
										queue.size(), queue.remainingCapacity() + queue.size(), fromFormat, toFormat,
										from, to, formatConverter));
		return conversationTask;
	}
}
