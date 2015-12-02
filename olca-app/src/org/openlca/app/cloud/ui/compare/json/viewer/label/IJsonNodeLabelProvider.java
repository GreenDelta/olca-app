package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;

public interface IJsonNodeLabelProvider {

	String getText(JsonNode node, Side side);

	Image getImage(JsonNode node, Side side);

}
