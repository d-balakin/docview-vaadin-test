package ru.rlisystems.docviewer.ui;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import static com.vaadin.server.Constants.PARAMETER_WIDGETSET;
import static com.vaadin.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.server.Constants.SERVLET_PARAMETER_UI_PROVIDER;

@WebServlet (value = "/*",
			 asyncSupported = true,
			 initParams = {
				 @WebInitParam (name = SERVLET_PARAMETER_PRODUCTION_MODE, value = "true"),
				 @WebInitParam (name = PARAMETER_WIDGETSET, value = "ru.rlisystems.docviewer.ui.DocumentViewer"),
				 @WebInitParam (name = SERVLET_PARAMETER_UI_PROVIDER, value = "com.vaadin.cdi.CDIUIProvider")
})
public class VaadinServlet extends com.vaadin.server.VaadinServlet
{ }
