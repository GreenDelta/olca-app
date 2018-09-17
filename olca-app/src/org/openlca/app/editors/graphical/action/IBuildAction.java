package org.openlca.app.editors.graphical.action;

import java.util.List;

import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.matrix.LinkingConfig.DefaultProviders;
import org.openlca.core.model.ProcessType;

interface IBuildAction {

	void setProcessNodes(List<ProcessNode> nodes);

	void setPreferredType(ProcessType preferredType);

	void setProviderMethod(DefaultProviders providers);

	String getText();

	void run();

}
