package org.openlca.app.editors.graphical.model;

import org.eclipse.swt.graphics.Color;

public interface Theme {

	Color boxColorOf(ProcessNode node);

	default Color boxBorderOf(ProcessNode node) {
		return boxColorOf(node);
	}

	Color boxHeaderForegroundOf(ProcessNode node);

	Color boxHeaderBackgroundOf(ProcessNode node);

	Color ioHeaderForegroundOf(ProcessNode node);

	Color ioInnerBackgroundOf(ProcessNode node);

	Color ioForegroundOf(ExchangeNode node);

	Color graphBackground();
}
