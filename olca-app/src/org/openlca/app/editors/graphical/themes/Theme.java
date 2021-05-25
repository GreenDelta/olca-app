package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;

public interface Theme {

	String label();

	String id();

	Color graphBorderColor();
	
	default Color boxBorderColorOf(ProcessNode node) {
		return graphBorderColor();
	}

	Color ioHeaderForegroundOf(ProcessNode node);

	Color ioForegroundOf(ExchangeNode node);

	Color graphBackground();
	
	default Color boxBackgroundOf(ProcessNode node) {
		return graphBackground();
	}
	
	Color graphForeground();
	
	default Color boxForegroundOf(ProcessNode node) {
		return graphForeground();
	}

	Color colorOf(Link link);

	Color defaultLinkColor();

}
