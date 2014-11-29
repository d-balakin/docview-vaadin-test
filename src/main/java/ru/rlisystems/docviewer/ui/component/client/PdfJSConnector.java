package ru.rlisystems.docviewer.ui.component.client;

import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import ru.rlisystems.docviewer.ui.component.PdfJS;

@Connect(PdfJS.class)
public class PdfJSConnector extends AbstractComponentConnector
{
	@Override
	protected PdfJSWidget createWidget ()
	{
		return new PdfJSWidget();
	}

	@Override
	public PdfJSWidget getWidget ()
	{
		return (PdfJSWidget) super.getWidget();
	}

	@Override
	public void onStateChanged (StateChangeEvent stateChangeEvent)
	{
		super.onStateChanged(stateChangeEvent);
		if (stateChangeEvent.hasPropertyChanged("resources")) {
			getWidget().setBaseUrl(getConnection().getThemeUri() + "/../../PdfJS/web/viewer.html");
			getWidget().setUrl(getResourceUrl("document"));
		}
	}
}
