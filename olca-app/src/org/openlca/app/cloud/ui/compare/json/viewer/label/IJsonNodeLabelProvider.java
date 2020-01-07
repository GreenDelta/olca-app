package org.openlca.app.cloud.ui.compare.json.viewer.label;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.diff.Site;

public interface IJsonNodeLabelProvider {

	String getText(JsonNode node, Site site);

	String getPropertyText(JsonNode node, Site site);

	String getValueText(JsonNode node, Site site);

	Image getImage(JsonNode node, Site site);

}
