package org.openlca.app.cloud.ui.compare.json;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.compare.json.DiffMatchPatch.Diff;
import org.openlca.app.cloud.ui.compare.json.DiffMatchPatch.Operation;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.AbstractViewer;

class JsonTree extends AbstractViewer<JsonNode, TreeViewer> {

	private boolean local;
	private boolean leftToRightCompare;
	private JsonTree counterpart;
	private IJsonNodeLabelProvider labelProvider;

	JsonTree(Composite parent, boolean local, boolean leftToRightCompare) {
		super(parent);
		this.local = local;
		this.leftToRightCompare = leftToRightCompare;
		if (local)
			getViewer().getTree().getVerticalBar().setVisible(false);
	}

	void setCounterpart(JsonTree counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	public void setLabelProvider(IJsonNodeLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
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
		return viewer;
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

	private class LabelProvider extends StyledCellLabelProvider {

		private final PropertyStyle propertyStyle = new PropertyStyle();
		private final DiffStyle diffStyle = new DiffStyle();
		private final ReadOnlyStyle readOnlyStyle = new ReadOnlyStyle();

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (!(element instanceof JsonNode))
				return;
			JsonNode node = (JsonNode) element;
			StyledString styledString = getStyledText(node);
			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			cell.setImage(labelProvider.getImage(node, local));
			super.update(cell);
		}

		private StyledString getStyledText(JsonNode node) {
			String text = labelProvider.getText(node, local);
			String otherText = labelProvider.getText(node, !local);
			text = adjustMultiline(node, text, otherText);
			StyledString styled = new StyledString(text);
			propertyStyle.applyTo(styled);
			if (node.readOnly)
				readOnlyStyle.applyTo(styled);
			if (!node.hasEqualValues()) {
				boolean highlightChanges = false;
				if (otherText != null)
					if (node.getElement().isJsonPrimitive())
						if (JsonUtil.toJsonPrimitive(node.getElement())
								.isString())
							highlightChanges = true;
				diffStyle.applyTo(styled, otherText, local, highlightChanges);
			}
			return styled;
		}

		private String adjustMultiline(JsonNode node, String value,
				String otherValue) {
			if (value == null)
				value = " ";
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

		@Override
		public void dispose() {
			if (readOnlyStyle.font != null)
				readOnlyStyle.font.dispose();
			super.dispose();
		}

	}

	private class PropertyStyle {

		private Styler styler = new Styler() {
			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.foreground = Colors.getLinkBlue();
			}
		};

		private void applyTo(StyledString styled) {
			String text = styled.getString();
			int index = text.indexOf(":");
			if (index == -1)
				styled.setStyle(0, text.length(), styler);
			else
				styled.setStyle(0, index + 1, styler);
		}

	}

	private class DiffStyle {

		private Styler deleteStyler = new DiffStyler(255, 230, 230, true);
		private Styler insertStyler = new DiffStyler(230, 255, 230, false);
		private Styler stringStyler = new DiffStyler(240, 240, 240, false);
		private Styler fieldStyler = new DiffStyler(255, 255, 128, false);

		private void applyTo(StyledString styled, String otherText,
				boolean local, boolean highlightChanges) {
			String text = styled.getString();
			int index = styled.getString().indexOf(":");
			Styler styler = highlightChanges ? stringStyler : fieldStyler;
			if (index == -1)
				styled.setStyle(0, text.length(), styler);
			else
				styled.setStyle(index + 2, text.length() - (index + 2), styler);
			if (highlightChanges)
				applySpecificDiffs(styled, otherText, local);
		}

		private void applySpecificDiffs(StyledString styled, String otherText,
				boolean local) {
			String text = styled.getString();
			DiffMatchPatch dmp = new DiffMatchPatch();
			LinkedList<Diff> diffs = null;
			if (local && !leftToRightCompare || !local && leftToRightCompare)
				diffs = dmp.diff_main(text, otherText);
			else
				diffs = dmp.diff_main(otherText, text);
			dmp.diff_cleanupSemantic(diffs);
			int index = 0;
			boolean showDelete = leftToRightCompare ? !local : local;
			boolean showInsert = leftToRightCompare ? local : !local;
			for (Diff diff : diffs) {
				if (showDelete && diff.operation == Operation.DELETE) {
					styled.setStyle(index, diff.text.length(), deleteStyler);
					index += diff.text.length();
				} else if (showInsert && diff.operation == Operation.INSERT) {
					styled.setStyle(index, diff.text.length(), insertStyler);
					index += diff.text.length();
				} else if (diff.operation == Operation.EQUAL)
					index += diff.text.length();
			}
		}

	}

	private class ReadOnlyStyle {

		private Font font;
		private Styler styler = new Styler() {

			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.foreground = Colors.getGray();
				textStyle.font = getFont();
			}
		};

		private Font getFont() {
			if (font != null)
				return font;
			FontDescriptor desc = FontDescriptor.createFrom(
					Display.getCurrent().getSystemFont()).setStyle(SWT.ITALIC);
			font = desc.createFont(Display.getCurrent());
			return font;
		}

		private void applyTo(StyledString styled) {
			String text = styled.getString();
			styled.setStyle(0, text.length(), styler);
		}

	}

	private class DiffStyler extends Styler {

		private final Color color;
		private final boolean strikeout;

		private DiffStyler(int r, int g, int b, boolean strikeout) {
			color = Colors.getColor(r, g, b);
			this.strikeout = strikeout;
		}

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.background = color;
			textStyle.strikeout = strikeout;
		}

	}

}