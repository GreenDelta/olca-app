package org.openlca.app.editors.graph.actions;

import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

import java.util.List;

interface IBuildAction {

	void setProcessNodes(List<Node> nodes);

	void setPreferredType(ProcessType preferredType);

	void setProviderMethod(ProviderLinking providerLinking);

	String getText();

	void run();

}
