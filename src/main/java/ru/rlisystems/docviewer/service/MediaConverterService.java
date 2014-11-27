package ru.rlisystems.docviewer.service;

import lombok.extern.java.Log;
import ru.rlisystems.docviewer.ConfigurationInjector.ConfigurationValue;
import ru.rlisystems.docviewer.converter.Format;
import ru.rlisystems.docviewer.converter.MediaConverter;
import ru.rlisystems.docviewer.converter.MediaConverter.ConversationTask;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Log
@ApplicationScoped
public class MediaConverterService
{
	@Inject
	private Instance<MediaConverter> mediaConverterInstance;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.max_pool", defaultValue = "-1")
	private int maximumPoolSize;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.pool_keepalive_ms", defaultValue = "60000")
	private int poolKeepAliveMs;

	@Inject @ConfigurationValue (name = "ru.rlisystems.docviewer.convert.pool_queue_cap", defaultValue = "100")
	private int poolQueueCapacity;

	@Resource
	private ManagedThreadFactory managedThreadFactory;

	private ExecutorService executorService;

	@PostConstruct
	private void postConstruct ()
	{
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(poolQueueCapacity);
		int maximumPoolSize = this.maximumPoolSize;
		if (maximumPoolSize < 1) {
			maximumPoolSize = Runtime.getRuntime().availableProcessors();
		}
		executorService = new ThreadPoolExecutor(0, maximumPoolSize, poolKeepAliveMs, TimeUnit.MILLISECONDS,
												 queue, managedThreadFactory);
	}

	public ConversationTask process (Format fromFormat, Format toFormat, File from, File to)
	{
		TreeSet<MediaConverter> applicableSet = new TreeSet<>((o1, o2) ->
														Float.compare(o1.getPriority(), o2.getPriority()));

		for (MediaConverter mediaConverter : mediaConverterInstance) {
			if (mediaConverter.isApplicable(fromFormat, toFormat)) {
				applicableSet.add(mediaConverter);
			}
		}
		if (applicableSet.isEmpty()) {
			throw new IllegalArgumentException("Конвертация " + from + " => " + to + " не поддерживается");
		}
		MediaConverter mediaConverter = applicableSet.last();
		ConversationTask conversationTask = mediaConverter.makeConversationTask(fromFormat, toFormat, from, to);
		executorService.submit(conversationTask);
		return conversationTask;
	}
}
