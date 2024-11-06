package org.openlca.app.collaboration.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.HistoryViewer;
import org.openlca.app.collaboration.viewers.diff.RefJson;
import org.openlca.app.collaboration.viewers.json.JsonCompareViewer;
import org.openlca.app.collaboration.viewers.json.olca.ModelDependencyResolver;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;

public class HistoryView extends ViewPart {

	public final static String ID = "views.collaboration.history";
	private static HistoryView instance;
	private HistoryViewer historyViewer;
	private AbstractTableViewer<Diff> referenceViewer;
	private JsonCompareViewer diffViewer;

	public HistoryView() {
		instance = this;
		setTitleImage(Icon.HISTORY_VIEW.get());
	}

	@Override
	public void createPartControl(Composite parent) {
		var body = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		UI.gridData(body, true, true);
		UI.gridLayout(body, 1);
		createHistoryViewer(body);
		var secondRow = new SashForm(body, SWT.HORIZONTAL | SWT.SMOOTH);
		createDiffViewer(secondRow);
		createReferenceViewer(secondRow);
		refresh();
	}

	private void createHistoryViewer(Composite parent) {
		historyViewer = new HistoryViewer(parent);
		UI.gridData(historyViewer.getViewer().getTable(), true, true);
		Tables.bindColumnWidths(historyViewer.getViewer(), 0.1, 0.7, 0.1, 0.1);
		historyViewer.addSelectionChangedListener((commit) -> {
			referenceViewer.select(null);
			var diffs = Repository.isConnected() && commit != null
					? Repository.CURRENT.diffs.find().commit(commit).withPreviousCommit()
					: new ArrayList<Diff>();
			referenceViewer.setInput(diffs);
		});
	}

	private void createReferenceViewer(Composite parent) {
		referenceViewer = new AbstractTableViewer<>(parent) {
			@Override
			protected IBaseLabelProvider getLabelProvider() {
				return new ReferenceLabel();
			};
		};
		UI.gridData(referenceViewer.getViewer().getTable(), true, true);
		referenceViewer.addSelectionChangedListener((diff) -> {
			if (diff == null || !Repository.isConnected() || diff.isCategory) {
				diffViewer.setInput(null);
				return;
			}
			var previousElement = RefJson.get(diff.oldRef);
			var currentElement = RefJson.get(diff.newRef);
			var node = new ModelNodeBuilder().build(previousElement, currentElement);
			diffViewer.setInput(node);
		});
	}

	private void createDiffViewer(Composite parent) {
		diffViewer = JsonCompareViewer.forComparison(parent, null, null);
		diffViewer.initialize(new ModelLabelProvider(), ModelDependencyResolver.INSTANCE);
	}

	public static void refresh() {
		if (instance == null)
			return;
		instance.historyViewer.setRepository(Repository.CURRENT);
	}

	@Override
	public void dispose() {
		instance = null;
		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	private static class ReferenceLabel extends BaseLabelProvider {

		@Override
		public String getText(Object element) {
			if (!(element instanceof Diff diff))
				return null;
			if (diff.isLibrary)
				return M.Libraries + "/" + diff.name;
			if (diff.isCategory)
				return diff.getCategoryPath();
			var text = diff.category;
			if (!text.isEmpty()) {
				text += "/";
			}
			var descriptor = Database.get().getDescriptor(diff.type.getModelClass(), diff.refId);
			if (descriptor != null)
				return text + descriptor.name;
			var ref = diff.oldRef == null ? diff.newRef : diff.oldRef;
			return text + Repository.CURRENT.datasets.getName(ref);
		}

		@Override
		public Image getImage(Object element) {
			if (!(element instanceof Diff diff))
				return null;
			Overlay overlay = null;
			if (diff.diffType == DiffType.ADDED) {
				overlay = Overlay.ADDED;
			} else if (diff.diffType == DiffType.DELETED) {
				overlay = Overlay.DELETED;
			}
			if (diff.isLibrary)
				return Images.library(overlay);
			if (diff.isCategory)
				return Images.getForCategory(diff.type, overlay);
			return Images.get(diff.type, overlay);
		}

	}

}
