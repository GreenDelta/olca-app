package org.openlca.app.editors.graphical.action;

import java.util.List;

import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.core.matrix.product.index.LinkingMethod;
import org.openlca.core.model.ProcessType;

interface IBuildAction {

	void setProcessNodes(List<ProcessNode> nodes);

	void setPreferredType(ProcessType preferredType);
	
	void setLinkingMethod(LinkingMethod linkingMethod);
	
	String getText();
	
	void run();

}
