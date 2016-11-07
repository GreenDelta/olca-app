package org.openlca.app.cloud.ui.compare.json.viewer;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.cloud.ui.compare.json.viewer.label.JsonTreeLabelProvider;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.AbstractViewer;

public class JsonTreeViewer extends AbstractViewer<JsonNode, TreeViewer> {

	private Side side;
	private Direction direction;

	public JsonTreeViewer(Composite parent, Side side, Direction direction) {
		super(parent, side);
		this.side = side;
		this.direction = direction;
	}

	@Override
	public TreeViewer getViewer() {
		return super.getViewer();
	}

	public void setCounterpart(JsonTreeViewer counterpart) {
		TreeViewer viewer = counterpart.getViewer();
		getViewer().addTreeListener(new ExpansionListener(viewer));
		getViewer().addSelectionChangedListener(
				new SelectionChangedListener(viewer));
		ScrollBar vBar = getViewer().getTree().getVerticalBar();
		vBar.addSelectionListener(new ScrollListener(viewer));
	}

	public void setLabelProvider(IJsonNodeLabelProvider labelProvider) {
		getViewer().setLabelProvider(
				new JsonTreeLabelProvider(labelProvider, side, direction));
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.NO_FOCUS | SWT.HIDE_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		Tree tree = viewer.getTree();
		if (viewerParameters[0] == Side.LEFT)
			tree.getVerticalBar().setVisible(false);
		UI.gridData(tree, true, true);
		return viewer;
	}

	public List<JsonNode> getSelection() {
		return Viewers.getAllSelected(getViewer());
	}

	@Override
	public void setInput(JsonNode[] input) {
		super.setInput(input);
	}

	public static enum Side {

		LEFT, RIGHT;

		public Side getOther() {
			if (this == LEFT)
				return RIGHT;
			return LEFT;
		}

	}

	public static enum Direction {

		LEFT_TO_RIGHT, RIGHT_TO_LEFT;

	}

}