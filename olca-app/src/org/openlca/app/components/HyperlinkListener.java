package org.openlca.app.components;

import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;

public class HyperlinkListener implements IHyperlinkListener {

	private Runnable onClick;
	
	public HyperlinkListener(Runnable onClick) {
		this.onClick = onClick;
	}
	
	@Override
	public final void linkEntered(HyperlinkEvent e) {

	}

	@Override
	public final void linkExited(HyperlinkEvent e) {

	}

	@Override
	public final void linkActivated(HyperlinkEvent e) {
		onClick.run();
	}
	
}
