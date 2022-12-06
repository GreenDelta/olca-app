package org.openlca.app.editors.graphical.actions;

import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;

import java.util.List;

interface IBuildAction {

	void setNodeParts(List<NodeEditPart> nodes);

	void setPreferredType(ProcessType preferredType);

	void setProviderMethod(ProviderLinking providerLinking);

	String getText();

	void run();

}
