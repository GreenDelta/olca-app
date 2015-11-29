package org.openlca.app.cloud.ui.compare.json;

import org.eclipse.swt.graphics.Image;

public interface IJsonNodeLabelProvider {
	
	String getText(JsonNode node, boolean local);

	Image getImage(JsonNode node, boolean local);

}
