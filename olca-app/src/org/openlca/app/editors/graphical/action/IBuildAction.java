package org.openlca.app.editors.graphical.action;

import java.util.List;

import org.openlca.app.editors.graphical.model.ProcessNode;

public interface IBuildAction {

	void setProcessNodes(List<ProcessNode> nodes);
	
	String getText();
	
	void run();

}
