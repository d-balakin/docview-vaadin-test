package ru.rlisystems.docviewer.ui.viewerjs;

import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractComponent;

public class ViewerJS extends AbstractComponent
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
