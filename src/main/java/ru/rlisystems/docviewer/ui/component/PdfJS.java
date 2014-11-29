package ru.rlisystems.docviewer.ui.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponent;

public class PdfJS extends AbstractComponent
{
	public Resource getDocument ()
	{
		return getResource("document");
	}

	public void setDocument (Resource document)
	{
		setResource("document", document);
	}
}
