package org.openlca.app.editors.projects;

import java.util.List;
import java.util.Objects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;

class ImpactSection {

	private ProjectEditor editor;
	private ImpactMethodViewer methodViewer;
	private NwSetComboViewer nwViewer;
	private IndicatorTable indicatorTable;

	public ImpactSection(ProjectEditor editor) {
		this.editor = editor;
	}

	public void render(Composite body, FormToolkit toolkit) {
		Composite composite = UI.formSection(body, toolkit, M.LCIAMethod, 1);
		Composite form = UI.formComposite(composite, toolkit);
		UI.gridLayout(form, 3);
		UI.gridData(form, true, false);
		createViewers(toolkit, form);
		indicatorTable = new IndicatorTable(editor);
		indicatorTable.render(composite);
		setInitialSelection();
		addListeners(); // do this after the initial selection to not set the
						// editor dirty
	}

	private void createViewers(FormToolkit toolkit, Composite composite) {
		UI.formLabel(composite, toolkit, M.LCIAMethod);
		methodViewer = new ImpactMethodViewer(composite);
		methodViewer.setNullable(true);
		methodViewer.setInput(Database.get());
		new CommentControl(composite, toolkit, "impactMethod", editor.getComments());
		UI.formLabel(composite, toolkit, M.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(composite);
		nwViewer.setNullable(true);
		nwViewer.setDatabase(Database.get());
		new CommentControl(composite, toolkit, "nwSet", editor.getComments());
	}

	private void addListeners() {
		methodViewer.addSelectionChangedListener(
				m -> onMethodChange(m));
		nwViewer.addSelectionChangedListener(nwset -> {
			Project project = editor.getModel();
			if (nwset == null) {
				project.nwSetId = null;
			} else {
				project.nwSetId = nwset.id;
			}
			editor.setDirty(true);
		});
	}

	private void onMethodChange(ImpactMethodDescriptor method) {
		Project project = editor.getModel();
		if (method == null && project.impactMethodId == null)
			return;
		if (method != null && Objects.equals(
				method.id, project.impactMethodId))
			return;
		project.nwSetId = null;
		project.impactMethodId = method == null
				? null
				: method.id;
		nwViewer.select(null);
		nwViewer.setInput(method);
		if (indicatorTable != null)
			indicatorTable.methodChanged(method);
		editor.setDirty(true);
	}

	private void setInitialSelection() {
		Project project = editor.getModel();
		if (project.impactMethodId == null)
			return;
		IDatabase db = Database.get();
		ImpactMethodDao methodDao = new ImpactMethodDao(db);
		ImpactMethodDescriptor method = methodDao.getDescriptor(
				project.impactMethodId);
		if (method == null)
			return;
		methodViewer.select(method);
		NwSetDao dao = new NwSetDao(db);
		List<NwSetDescriptor> nwSets = dao.getDescriptorsForMethod(method.id);
		nwViewer.setInput(nwSets);
		nwViewer.select(getInitialNwSet(project, nwSets));
	}

	private NwSetDescriptor getInitialNwSet(Project project,
			List<NwSetDescriptor> nwSets) {
		if (project.nwSetId == null)
			return null;
		for (NwSetDescriptor d : nwSets)
			if (project.nwSetId == d.id)
				return d;
		return null;
	}

}
