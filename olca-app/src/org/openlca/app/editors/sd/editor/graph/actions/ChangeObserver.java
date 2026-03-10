package org.openlca.app.editors.sd.editor.graph.actions;

@FunctionalInterface
interface ChangeObserver {
	void reactOn(boolean panelValid);
}
