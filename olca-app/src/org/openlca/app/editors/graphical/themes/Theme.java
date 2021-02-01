package org.openlca.app.editors.graphical.themes;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;

public interface Theme {

	String label();

	String id();

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
