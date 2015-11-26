package org.openlca.app.cloud.ui.compare;

import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.AbstractViewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModelTree extends AbstractViewer<JsonNode, TreeViewer> {

	private boolean local;
	private ModelTree counterpart;

	ModelTree(Composite parent, boolean local) {
		super(parent);
		this.local = local;
	}

	void setCounterpart(ModelTree counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.NO_FOCUS
				| SWT.HIDE_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		viewer.getTree().getVerticalBar()
				.addSelectionListener(new ScrollListener());
		viewer.addSelectionChangedListener(new SelectionChangedListener());
		viewer.addTreeListener(new ExpansionListener());
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		if (local)
			tree.getVerticalBar().setVisible(false);
		return viewer;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private void updateOtherScrollBar() {
		ScrollBar bar = this.getViewer().getTree().getVerticalBar();
		ScrollBar otherBar = counterpart.getViewer().getTree().getVerticalBar();
		otherBar.setSelection(bar.getSelection());
		if (local)
			bar.setVisible(false);
		else
			otherBar.setVisible(false);
	}

	public List<JsonNode> getSelection() {
		return Viewers.getAllSelected(getViewer());
	}

	private class ScrollListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent e) {
			updateOtherScrollBar();
		}

	}

	private class ExpansionListener implements ITreeViewerListener {

		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			setExpanded(event.getElement(), true);
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			setExpanded(event.getElement(), false);
		}

		private void setExpanded(Object element, boolean value) {
			if (!(element instanceof JsonNode))
				return;
			JsonNode node = (JsonNode) element;
			counterpart.getViewer().setExpandedState(node, value);
			updateOtherScrollBar();
		}

	}

	private class SelectionChangedListener implements ISelectionChangedListener {

		private boolean pauseListening;

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (pauseListening)
				return;
			pauseListening = true;
			counterpart.getViewer().setSelection(event.getSelection());
			pauseListening = false;
		}

	}

	private class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof Object[]))
				return null;
			Object[] array = (Object[]) inputElement;
			if (array.length == 0)
				return null;
			if (!(array[0] instanceof JsonNode))
				return null;
			return ((JsonNode) array[0]).children.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) parentElement;
			return node.children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			JsonNode node = (JsonNode) element;
			return node.parent;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof JsonNode))
				return false;
			JsonNode node = (JsonNode) element;
			return !node.children.isEmpty();
		}

	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements IColorProvider {

		@Override
		public String getText(Object obj) {
			if (!(obj instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) obj;
			String text = getText(node, local);
			return adjustMultiline(node, text, local);
		}

		private String getText(JsonNode node, boolean local) {
			JsonElement element = local ? node.getLocalElement() : node
					.getRemoteElement();
			if (element == null)
				if (isEmptyArrayElement(node))
					return null;
				else if (isEmptyArrayElement(node.parent))
					return null;
				else if (isEmptyElement(node.parent))
					return null;
				else
					return node.key + ": " + getValue(node, null, local);
			if (element.isJsonNull())
				return node.key + ": " + getValue(node, null, local);
			if (element.isJsonArray())
				if (element.getAsJsonArray().size() == 0)
					return node.key + ": " + getValue(node, null, local);
				else
					return node.key;
			if (element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				JsonElement parent = local ? node.parent.getLocalElement()
						: node.parent.getRemoteElement();
				if (!JsonUtil.isReference(parent, object))
					return node.key;
				String value = getValue(node, object.get("name").getAsString(),
						local);
				return node.key + ": " + value;
			}
			String value = getValue(node, element.getAsString(), local);
			return node.key + ": " + value;
		}

		private String getValue(JsonNode node, String value, boolean local) {
			JsonElement parent = null;
			if (node.parent != null)
				parent = local ? node.parent.getLocalElement() : node.parent
						.getRemoteElement();
			if (EnumUtil.isEnum(parent, node.key)) {
				Object enumValue = EnumUtil.getEnum(parent, node.key, value);
				value = Labels.getEnumText(enumValue);
			}
			if (value != null)
				return value;
			return "null";
		}

		private String adjustMultiline(JsonNode node, String value,
				boolean local) {
			String otherValue = getText(node, !local);
			int count1 = countLines(value);
			int count2 = countLines(otherValue);
			if (count2 > count1)
				for (int i = 1; i <= (count2 - count1); i++)
					value += "\n";
			return value;
		}

		private int countLines(String value) {
			if (value == null)
				return 0;
			int index = -1;
			int count = 0;
			while ((index = value.indexOf("\n", index + 1)) != -1)
				count++;
			return count;
		}

		private boolean isEmptyArrayElement(JsonNode node) {
			JsonElement element = local ? node.getLocalElement() : node
					.getRemoteElement();
			if (element != null)
				return false;
			if (node.parent != null && node.parent.getElement() != null)
				return node.parent.getElement().isJsonArray();
			return false;
		}

		private boolean isEmptyElement(JsonNode node) {
			JsonElement element = local ? node.getLocalElement() : node
					.getRemoteElement();
			if (element != null)
				return false;
			return true;
		}

		@Override
		public Color getForeground(Object object) {
			return null;
		}

		@Override
		public Color getBackground(Object object) {
			if (!(object instanceof JsonNode))
				return null;
			JsonNode node = (JsonNode) object;
			if (node.hasEqualValues())
				return null;
			return Colors.getColor(255, 255, 128);
		}

		@Override
		public Image getImage(Object object) {
			return null;
		}

	}

}