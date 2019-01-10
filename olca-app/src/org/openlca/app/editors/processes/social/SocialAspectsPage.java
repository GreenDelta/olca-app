package org.openlca.app.editors.processes.social;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.database.SocialIndicatorDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class SocialAspectsPage extends ModelPage<Process> {

	private ProcessEditor editor;

	private TreeViewer tree;
	private TreeModel treeModel = new TreeModel();
	private ScrolledForm form;

	public SocialAspectsPage(ProcessEditor editor) {
		super(editor, "SocialAspectsPage", M.SocialAspects);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		for (SocialAspect a : getModel().socialAspects)
			treeModel.addAspect(a);
		form = UI.formHeader(this);
		FormToolkit tk = managedForm.getToolkit();
		Composite body = UI.formBody(form, tk);
		createEntrySection(tk, body);
		form.reflow(true);
	}

	private void createEntrySection(FormToolkit tk, Composite body) {
		Section section = UI.section(body, tk, M.SocialAssessment);
		Composite comp = UI.sectionClient(section, tk, 1);
		UI.gridData(section, true, true);
		createTree(comp);
		Trees.onDoubleClick(tree, (e) -> editAspect());
		Action add = Actions.onAdd(this::addIndicator);
		Action edit = Actions.create(M.Edit, Icon.EDIT.descriptor(), this::editAspect);
		Action delete = Actions.onRemove(this::deleteAspect);
		CommentAction.bindTo(section, "socialAspects", editor.getComments(), add, edit, delete);
		Actions.bind(tree, add, edit, delete);
	}

	private void editAspect() {
		Object o = Viewers.getFirstSelected(tree);
		if (!(o instanceof SocialAspect))
			return;
		SocialAspect copy = ((SocialAspect) o).clone();
		if (Dialog.open(copy, editor.getModel().socialDqSystem) == Window.OK) {
			Aspects.update(getModel(), copy);
			treeModel.update(copy);
			tree.refresh();
			editor.setDirty(true);
		}
	}

	private void createTree(Composite comp) {
		List<String> headers = new ArrayList<>(Arrays.asList(M.Name, M.RawValue, M.RiskLevel, M.ActivityVariable,
				M.DataQuality, M.Comment, M.Source));
		if (editor.hasAnyComment("socialAspects")) {
			headers.add("");
		}
		tree = Trees.createViewer(comp, headers.toArray(new String[headers.size()]), new TreeLabel(editor));
		tree.setContentProvider(new TreeContent());
		tree.setAutoExpandLevel(3);
		tree.setInput(treeModel);
		new ModifySupport<SocialAspect>(tree).bind("", new CommentDialogModifier<SocialAspect>(
				editor.getComments(), CommentPaths::get));
		Trees.bindColumnWidths(tree.getTree(), 0.2, 0.15, 0.15, 0.15, 0.12, 0.1, 0.1);
	}

	private void addIndicator() {
		BaseDescriptor[] list = ModelSelectionDialog.multiSelect(ModelType.SOCIAL_INDICATOR);
		if (list == null)
			return;
		for (BaseDescriptor d : list) {
			SocialAspect aspect = Aspects.find(getModel(), d);
			if (aspect != null)
				continue;
			addIndicator(d);
		}
	}

	private void addIndicator(BaseDescriptor d) {
		SocialIndicatorDao dao = new SocialIndicatorDao(Database.get());
		SocialIndicator i = dao.getForId(d.id);
		SocialAspect a = new SocialAspect();
		a.indicator = i;
		getModel().socialAspects.add(a);
		treeModel.addAspect(a);
		tree.refresh();
		editor.setDirty(true);
	}

	private void deleteAspect() {
		Object o = Viewers.getFirstSelected(tree);
		if (!(o instanceof SocialAspect))
			return;
		SocialAspect a = (SocialAspect) o;
		Aspects.remove(getModel(), a);
		treeModel.remove(a);
		tree.refresh();
		editor.setDirty(true);
	}
}
