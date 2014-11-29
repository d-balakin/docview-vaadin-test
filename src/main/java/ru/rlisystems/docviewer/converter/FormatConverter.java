package ru.rlisystems.docviewer.converter;

import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

public interface FormatConverter
{
	boolean isApplicable (Format fromFormat, Format toFormat);
	ConversationTask makeConversationTask (Format fromFormat, Format toFormat, File from, File to);

	default String getConverterClass ()
	{
		return "default";
	}

	public static interface ConversationTask extends Runnable
	{
		void addConversationEventListener(ConversationTaskEventListener conversationEventListener);
		void removeConversationEventListener(ConversationTaskEventListener conversationEventListener);
	}

	public static @Data	class ConversationTaskEvent
	{
		private final State state;

		public static enum State {
			IDLE, IN_PROCESS, COMPLETED, FAILURE
		}
	}

	public static interface ConversationTaskEventListener extends EventListener
	{
		void onConversationTaskEvent (ConversationTaskEvent conversationTaskEvent);
	}

	public static abstract class AbstractConversationTask implements ConversationTask
	{
		private final List<ConversationTaskEventListener> listeners = Collections.synchronizedList(new ArrayList<>());
		private final List<ConversationTaskEvent> eventBuffer = new ArrayList<>();
		private ConversationTaskEvent.State state;

		protected AbstractConversationTask ()
		{
			setState(ConversationTaskEvent.State.IDLE);
		}

		@Override
		public void addConversationEventListener (ConversationTaskEventListener conversationEventListener)
		{
			synchronized (listeners) {
				listeners.add(conversationEventListener);
				eventBuffer.forEach(conversationEventListener::onConversationTaskEvent);
			}
		}

		@Override
		public void removeConversationEventListener (ConversationTaskEventListener conversationEventListener)
		{
			listeners.remove(conversationEventListener);
		}

		public ConversationTaskEvent.State getState ()
		{
			return state;
		}

		public void setState (ConversationTaskEvent.State state)
		{
			if (this.state != state) {
				this.state = state;
				ConversationTaskEvent conversationTaskEvent = new ConversationTaskEvent(state);
				synchronized (listeners) {
					eventBuffer.add(conversationTaskEvent);
					listeners.forEach(l -> l.onConversationTaskEvent(conversationTaskEvent));
				}
			}
			else {
				this.state = state;
			}
		}
	}
}

