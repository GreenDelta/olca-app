package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.commons.Strings;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.descriptors.Descriptor;

public class SocialAspectsPage extends ModelPage<Process> {

	private final ProcessEditor editor;

	private TreeViewer tree;
	private final TreeModel treeModel;

	public SocialAspectsPage(ProcessEditor editor) {
		super(editor, "SocialAspectsPage", M.SocialAspects);
		this.editor = editor;
		this.treeModel = TreeModel.of(editor.getModel());
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		createEntrySection(tk, body);
		form.reflow(true);
	}

	private void createEntrySection(FormToolkit tk, Composite body) {
		var section = UI.section(body, tk, M.SocialAssessment);
		var comp = UI.sectionClient(section, tk, 1);
		UI.gridData(section, true, true);
		createTree(comp);
		if (!isEditable())
			return;
		Trees.onDoubleClick(tree, (e) -> editAspect());
		var add = Actions.onAdd(() -> addIndicators(
			ModelSelector.multiSelect(ModelType.SOCIAL_INDICATOR)));
		var edit = Actions.create(M.Edit, Icon.EDIT.descriptor(), this::editAspect);
		var delete = Actions.onRemove(this::deleteAspects);
		CommentAction.bindTo(
			section, "socialAspects", editor.getComments(), add, edit, delete);
		Actions.bind(tree, add, edit, delete);
		ModelTransfer.onDrop(tree.getTree(), this::addIndicators);
	}

	private void editAspect() {
		var o = Viewers.getFirstSelected(tree);
		if (!(o instanceof SocialAspect aspect)) {
			addIndicators(ModelSelector.multiSelect(ModelType.SOCIAL_INDICATOR));
			return;
		}
		var copy = aspect.copy();
		if (Dialog.open(copy, editor.getModel().socialDqSystem) == Window.OK) {
			Aspects.update(getModel(), copy);
			treeModel.update(copy);
			tree.refresh();
			editor.setDirty(true);
		}
	}

	private void createTree(Composite comp) {
		var headers = new ArrayList<>(List.of(
			M.Name, M.RawValue, M.RiskLevel, M.ActivityVariable,
			M.DataQuality, M.Comment, M.Source));
		if (editor.hasAnyComment("socialAspects")) {
			headers.add("");
		}
		tree = Trees.createViewer(
			comp, headers.toArray(new String[0]), new TreeLabel(editor));
		tree.setContentProvider(new TreeContent());
		tree.setAutoExpandLevel(3);
		tree.setInput(treeModel);
		tree.setComparator(new Comparator());
		new ModifySupport<SocialAspect>(tree).bind(
			"", new CommentDialogModifier<>(editor.getComments(), CommentPaths::get));
		Trees.bindColumnWidths(tree.getTree(), 0.2, 0.15, 0.15, 0.15, 0.12, 0.1, 0.1);
	}

	private void addIndicators(List<? extends Descriptor> ds) {
		if (ds == null || ds.isEmpty())
			return;
		for (var d : ds) {
			if (d.type != ModelType.SOCIAL_INDICATOR)
				continue;
			var aspect = Aspects.find(getModel(), d);
			if (aspect == null) {
				addIndicator(d);
			}
		}
	}

	private void addIndicator(Descriptor d) {
		var indicator = Database.get().get(SocialIndicator.class, d.id);
		if (indicator == null)
			return;
		var aspect = new SocialAspect();
		aspect.indicator = indicator;
		getModel().socialAspects.add(aspect);
		treeModel.addAspect(aspect);
		tree.refresh();
		editor.setDirty(true);
	}

	private void deleteAspects() {
		var aspects = new ArrayList<SocialAspect>();
		for (var obj : Viewers.getAllSelected(tree)) {
			switch (obj) {
				case SocialAspect a -> aspects.add(a);
				case CategoryNode n -> aspects.addAll(n.getAllAscpectsRecursively());
				case null, default -> {
				}
			}
		}
		if (aspects.isEmpty())
			return;
		var process = getModel();
		for (var aspect : aspects) {
			Aspects.remove(process, aspect);
			treeModel.remove(aspect);
		}
		treeModel.dropEmptyCategories();
		tree.refresh();
		editor.setDirty(true);
	}

	private static class Comparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object objA, Object objB) {
			return switch (objA) {

				case CategoryNode ca -> switch (objB) {
					case CategoryNode cb -> Strings.compareIgnoreCase(
						ca.name(), cb.name());
					case SocialAspect ignore -> 0;
					case null, default -> super.compare(viewer, objA, objB);
				};

				case SocialAspect aa -> switch (objB) {
					case CategoryNode cb -> 1;
					case SocialAspect ab -> Strings.compareIgnoreCase(
						Labels.name(aa.indicator), Labels.name(ab.indicator));
					case null, default -> super.compare(viewer, objA, objB);
				};

				case null, default -> super.compare(viewer, objA, objB);
			};
		}
	}
}
