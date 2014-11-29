package ru.rlisystems.docviewer.ui.component.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;

public class PdfJSWidget extends Widget
{
	private String baseUrl;
	private DivElement divElement;
	private IFrameElement iFrameElement;

	public PdfJSWidget ()
	{
		divElement = Document.get().createDivElement();
		divElement.addClassName("v-pdfjs");
		setElement(divElement);
	}

	public void setUrl (String url)
	{
		IFrameElement oldIFrameElement = iFrameElement;
		iFrameElement = null;
		if (url != null && !url.isEmpty()) {
			iFrameElement = Document.get().createIFrameElement();
			iFrameElement.getStyle().setBorderStyle(Style.BorderStyle.NONE);
			iFrameElement.setAttribute("allowfullscreen", "");
			iFrameElement.setAttribute("src", baseUrl + "?file=" + url);
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

	public void setBaseUrl (String baseUrl)
	{
		this.baseUrl = baseUrl;
	}
}
