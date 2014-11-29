package ru.rlisystems.docviewer.ui;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import javax.inject.Inject;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import static com.vaadin.server.Constants.*;

@Push
@CDIUI
@Theme ("mytheme")
public class DocumentViewerUI extends UI
{
	@Inject
	private Workspace workspace;

	@Override
	protected void init (VaadinRequest request)
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.addComponent(workspace);
		workspace.setWidth(90, Unit.PERCENTAGE);
		workspace.setHeight(90, Unit.PERCENTAGE);
		layout.setComponentAlignment(workspace, Alignment.MIDDLE_CENTER);
		setContent(layout);
	}
}
