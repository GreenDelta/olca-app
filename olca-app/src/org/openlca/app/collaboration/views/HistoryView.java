package org.openlca.app.collaboration.views;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.viewers.HistoryViewer;
import org.openlca.app.collaboration.viewers.json.JsonCompareViewer;
import org.openlca.app.collaboration.viewers.json.olca.ModelDependencyResolver;
import org.openlca.app.collaboration.viewers.json.olca.ModelLabelProvider;
import org.openlca.app.collaboration.viewers.json.olca.ModelNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;
import org.openlca.git.util.Diffs;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class HistoryView extends ViewPart {

	public final static String ID = "views.collaboration.history";
	private static HistoryView instance;
	private HistoryViewer historyViewer;
	private AbstractTableViewer<Diff> referenceViewer;
	private JsonCompareViewer diffViewer;
	private Gson gson = new Gson();

	public HistoryView() {
		instance = this;
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
					? Diffs.of(Repository.get().git, commit).withPreviousCommit()
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
			if (diff == null || !Repository.isConnected()) {
				diffViewer.setInput(null);
				return;
			}
			var previousElement = getJson(diff.toReference(Side.OLD));
			var currentElement = getJson(diff.toReference(Side.NEW));
			var node = new ModelNodeBuilder().build(previousElement, currentElement);
			diffViewer.setInput(node);
		});
	}

	private void createDiffViewer(Composite parent) {
		diffViewer = JsonCompareViewer.forComparison(parent, null, null);
		diffViewer.initialize(new ModelLabelProvider(), ModelDependencyResolver.INSTANCE);
	}

	private JsonObject getJson(Reference ref) {
		if (ref == null || ObjectId.zeroId().equals(ref.objectId))
			return null;
		var datasets = Repository.get().datasets;
		var json = datasets.get(ref.objectId);
		if (Strings.nullOrEmpty(json))
			return null;
		var obj = gson.fromJson(json, JsonObject.class);
		if (ref.type != ModelType.PROCESS)
			return obj;
		var exchanges = obj.get("exchanges");
		if (exchanges == null || !exchanges.isJsonArray())
			return obj;
		var inputs = new JsonArray();
		var outputs = new JsonArray();
		for (var elem : exchanges.getAsJsonArray()) {
			if (!elem.isJsonObject())
				continue;
			var e = elem.getAsJsonObject();
			var f = e.get("isInput");
			var isInput = f.isJsonPrimitive() && f.getAsBoolean() == true;
			if (isInput) {
				inputs.add(elem);
			} else {
				outputs.add(elem);
			}
		}
		if (!inputs.isEmpty()) {
			obj.add("inputs", inputs);
		}
		if (!outputs.isEmpty()) {
			obj.add("outputs", outputs);
		}
		obj.remove("exchanges");
		return obj;
	}

	public static void refresh() {
		if (instance == null)
			return;
		instance.historyViewer.setRepository(Repository.get());
	}

	@Override
	public void dispose() {
		instance = null;
		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	private class ReferenceLabel extends BaseLabelProvider {

		@Override
		public String getText(Object element) {
			if (!(element instanceof Diff))
				return null;
			var diff = (Diff) element;
			var text = diff.category;
			if (!text.isEmpty()) {
				text += "/";
			}
			var descriptor = Database.get().getDescriptor(diff.type.getModelClass(), diff.refId);
			if (descriptor != null)
				return text + descriptor.name;
			var objectId = ObjectId.zeroId().equals(diff.oldObjectId) ? diff.newObjectId : diff.oldObjectId;
			return text + Repository.get().datasets.getName(objectId);
		}

		@Override
		public Image getImage(Object element) {
			if (!(element instanceof Diff))
				return null;
			var data = (Diff) element;
			Overlay overlay = null;
			if (data.diffType == DiffType.ADDED) {
				overlay = Overlay.ADDED;
			} else if (data.diffType == DiffType.DELETED) {
				overlay = Overlay.DELETED;
			}
			return Images.get(data.type, overlay);
		}

	}

}
