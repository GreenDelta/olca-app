package org.openlca.app.editors.processes.social;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.util.trees.Trees;
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

	public SocialAspectsPage(ProcessEditor editor) {
		super(editor, "SocialAspectsPage", Messages.SocialAspects);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		for (SocialAspect a : getModel().socialAspects)
			treeModel.addAspect(a);
		ScrolledForm form = UI.formHeader(mform, Messages.SocialAspects);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createEntrySection(tk, body);
		form.reflow(true);
	}

	private void createEntrySection(FormToolkit tk, Composite body) {
		Section section = UI.section(body, tk, Messages.SocialAssessment);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridData(section, true, true);
		UI.gridLayout(comp, 1);
		createTree(comp);
		Trees.onDoubleClick(tree, (e) -> editAspect());
		Action add = Actions.onAdd(this::addIndicator);
		Action edit = Actions.create(Messages.Edit,
				ImageType.EDIT_16.getDescriptor(), this::editAspect);
		Action delete = Actions.onRemove(this::deleteAspect);
		Actions.bind(section, add, edit, delete);
		Actions.bind(tree, add, edit, delete);
	}

	private void editAspect() {
		Object o = Viewers.getFirstSelected(tree);
		if (!(o instanceof SocialAspect))
			return;
		SocialAspect copy = ((SocialAspect) o).clone();
		if (Dialog.open(copy) == Window.OK) {
			Aspects.update(getModel(), copy);
			treeModel.update(copy);
			tree.refresh();
			editor.setDirty(true);
		}
	}

	private void createTree(Composite comp) {
		String[] headers = { Messages.Name, Messages.RawValue,
				Messages.RiskLevel, Messages.ActivityVariable,
				Messages.DataQuality, Messages.Comment, Messages.Source };
		tree = new TreeViewer(comp, SWT.FULL_SELECTION
				| SWT.MULTI | SWT.BORDER);
		Tree t = tree.getTree();
		for (int i = 0; i < headers.length; i++) {
			TreeColumn c = new TreeColumn(t, SWT.NULL);
			c.setText(headers[i]);
		}
		for (TreeColumn c : t.getColumns())
			c.pack();
		tree.setColumnProperties(headers);
		Trees.bindColumnWidths(t, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
		tree.setAutoExpandLevel(3);
		t.setLinesVisible(false);
		t.setHeaderVisible(true);
		UI.gridData(t, true, true);
		tree.setContentProvider(new TreeContent());
		tree.setLabelProvider(new TreeLabel());
		tree.setInput(treeModel);
	}

	private void addIndicator() {
		BaseDescriptor[] list = ModelSelectionDialog
				.multiSelect(ModelType.SOCIAL_INDICATOR);
		for (BaseDescriptor d : list) {
			SocialAspect aspect = Aspects.find(getModel(), d);
			if (aspect == null)
				addIndicator(d);
		}
	}

	private void addIndicator(BaseDescriptor d) {
		SocialIndicatorDao dao = new SocialIndicatorDao(Database.get());
		SocialIndicator i = dao.getForId(d.getId());
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
