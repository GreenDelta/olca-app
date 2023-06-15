package org.openlca.app.editors.processes.social;

import org.eclipse.jface.viewers.TreeViewer;
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
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.descriptors.Descriptor;

import java.util.ArrayList;
import java.util.List;

public class SocialAspectsPage extends ModelPage<Process> {

	private final ProcessEditor editor;

	private TreeViewer tree;
	private final TreeModel treeModel = new TreeModel();

	public SocialAspectsPage(ProcessEditor editor) {
		super(editor, "SocialAspectsPage", M.SocialAspects);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		for (var aspect : getModel().socialAspects) {
			treeModel.addAspect(aspect);
		}
		var form = UI.header(this);
		var tk = managedForm.getToolkit();
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
		var delete = Actions.onRemove(this::deleteAspect);
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

	private void deleteAspect() {
		var o = Viewers.getFirstSelected(tree);
		if (!(o instanceof SocialAspect aspect))
			return;
		Aspects.remove(getModel(), aspect);
		treeModel.remove(aspect);
		tree.refresh();
		editor.setDirty(true);
	}
}
