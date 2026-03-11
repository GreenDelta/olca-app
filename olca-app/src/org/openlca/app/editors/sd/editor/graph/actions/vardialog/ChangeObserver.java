package org.openlca.app.editors.sd.editor.graph.actions.vardialog;

@FunctionalInterface
interface ChangeObserver {
	void reactOn(boolean panelValid);
}
