package org.openlca.app.tools.graphics.frame;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.tools.graphics.BasicGraphicalEditor;
import org.openlca.app.tools.graphics.actions.MinimapAction;

public abstract class GraphicalEditorWithFrame extends BasicGraphicalEditor {

	private Minimap minimap;
	private Header header;

	@Override
	public void createPartControl(Composite parent) {
		var splitter = new Splitter(parent, SWT.NONE);
		super.createPartControl(splitter);
		createMinimap(splitter);
		createHeader(splitter);
	}

	public void createMinimap(Splitter parent) {
		var minimap = new Minimap(parent, SWT.TRANSPARENT);
		setMinimap(minimap);
		minimap.setRootEditPart(getRootEditPart());
		minimap.initialize();
	}

	public void createHeader(Splitter parent) {}

	@Override
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();
		IAction action;

		action = new MinimapAction(this);
		registry.registerAction(action);
	}

	public Minimap getMinimap() {
		return minimap;
	}

	public Header getHeader() {
		return header;
	}

	public void setMinimap(Minimap map) {
		minimap = map;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

}
