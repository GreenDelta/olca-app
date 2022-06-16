package org.openlca.app.collaboration.viewers.json.label;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;

public interface IJsonNodeLabelProvider {

	String getText(JsonNode node, Side side);

	String getPropertyText(JsonNode node, Side side);

	String getValueText(JsonNode node, Side side);

	Image getImage(JsonNode node, Side side);

}
