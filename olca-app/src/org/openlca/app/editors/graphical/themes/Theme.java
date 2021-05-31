package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;

public interface Theme {

	String label();

	String id();

	Color defaultFontColor();

	Color defaultBackgroundColor();

	Color defaultBorderColor();

	Color defaultLinkColor();

	Color infoFontColor();

	default Color fontColorOf(ProcessNode node) {
		return defaultFontColor();
	}

	default Color borderColorOf(ProcessNode node) {
		return defaultBorderColor();
	}

	default Color backgroundColorOf(ProcessNode node) {
		return defaultBackgroundColor();
	}

	default Color fontColorOf(ExchangeNode node) {
		return defaultFontColor();
	}

	default Color linkColorOf(Link link) {
		return defaultLinkColor();
	}
}
