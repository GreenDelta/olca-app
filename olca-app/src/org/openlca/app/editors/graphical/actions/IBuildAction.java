package org.openlca.app.editors.graphical.actions;

import org.openlca.app.editors.graphical.model.Node;
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
