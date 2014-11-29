package ru.rlisystems.docviewer.ui.container;

import com.vaadin.data.util.BeanContainer;

public class BeanItemMemoryContainer<K, V> extends BeanContainer<K, V> implements BeanItemContainer<K, V>
{
	public BeanItemMemoryContainer (Class<? super V> type)
	{
		super(type);
	}
}
