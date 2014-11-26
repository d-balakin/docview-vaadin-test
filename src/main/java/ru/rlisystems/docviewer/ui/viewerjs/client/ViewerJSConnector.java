package ru.rlisystems.docviewer.ui.viewerjs.client;

import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import ru.rlisystems.docviewer.ui.viewerjs.ViewerJS;

@Connect (ViewerJS.class)
public class ViewerJSConnector extends AbstractComponentConnector
{
	@Override
	protected ViewerJSWidget createWidget ()
	{
		return new ViewerJSWidget();
	}

	@Override
	public ViewerJSWidget getWidget ()
	{
		return (ViewerJSWidget) super.getWidget();
	}

	@Override
	public void onStateChanged (StateChangeEvent stateChangeEvent)
	{
		super.onStateChanged(stateChangeEvent);
		if (stateChangeEvent.hasPropertyChanged("resources")) {
			getWidget().setBaseUrl(getConnection().getThemeUri() + "/../../ViewerJS/index.html");
			getWidget().setUrl(getResourceUrl("document"));
		}
	}
}
