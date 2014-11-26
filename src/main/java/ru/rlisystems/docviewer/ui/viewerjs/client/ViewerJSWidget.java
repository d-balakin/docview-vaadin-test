package ru.rlisystems.docviewer.ui.viewerjs.client;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.Widget;

public class ViewerJSWidget extends Widget
{
	private String baseUrl;
	private DivElement divElement;
	private IFrameElement iFrameElement;

	public ViewerJSWidget ()
	{
		divElement = Document.get().createDivElement();
		setElement(divElement);
	}

	public void setUrl (String url)
	{
		IFrameElement oldIFrameElement = iFrameElement;
		iFrameElement = null;
		if (url != null && !url.isEmpty()) {
			iFrameElement = Document.get().createIFrameElement();
			iFrameElement.setAttribute("allowfullscreen", "");
			iFrameElement.setAttribute("src", baseUrl + "#" + url);
			iFrameElement.getStyle().setWidth(100, Style.Unit.PCT);
			iFrameElement.getStyle().setHeight(100, Style.Unit.PCT);
		}
		if (oldIFrameElement != null && iFrameElement != null) {
			divElement.replaceChild(iFrameElement, oldIFrameElement);
		}
		else {
			if (oldIFrameElement != null) {
				divElement.removeChild(oldIFrameElement);
			}
			if (iFrameElement != null) {
				divElement.appendChild(iFrameElement);
			}
		}
	}

	public String getBaseUrl ()
	{
		return baseUrl;
	}

	public void setBaseUrl (String baseUrl)
	{
		this.baseUrl = baseUrl;
	}
}
