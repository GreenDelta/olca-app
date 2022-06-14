package org.openlca.app.editors.graphical_legacy.action;

import java.util.List;

import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

interface IBuildAction {

	void setProcessNodes(List<ProcessNode> nodes);

	void setPreferredType(ProcessType preferredType);

	void setProviderMethod(ProviderLinking providerLinking);

	String getText();

	void run();

}
