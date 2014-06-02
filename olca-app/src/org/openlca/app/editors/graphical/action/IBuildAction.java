package org.openlca.app.editors.graphical.action;

import org.openlca.app.editors.graphical.model.ProcessNode;

public interface IBuildAction {

	void setProcessNode(ProcessNode node);
	
	String getText();
	
	void run();

}
