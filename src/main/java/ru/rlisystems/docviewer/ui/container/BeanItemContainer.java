package ru.rlisystems.docviewer.ui.container;

import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItem;

public interface BeanItemContainer<K, V> extends Container
{
	@Override
	BeanItem<V> getItem (Object itemId);
}
