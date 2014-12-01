package org.openlca.app.editors.projects;

import java.util.List;
import java.util.Objects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
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
		Composite composite = UI
				.formSection(body, toolkit, Messages.LCIAMethod);
		UI.gridLayout(composite, 1);
		Composite form = UI.formComposite(composite, toolkit);
		UI.gridData(form, true, false);
		createViewers(toolkit, form);
		indicatorTable = new IndicatorTable(editor);
		indicatorTable.render(composite);
		setInitialSelection();
		addListeners(); // do this after the initial selection to not set the
						// editor dirty
	}

	private void createViewers(FormToolkit toolkit, Composite composite) {
		UI.formLabel(composite, toolkit, Messages.LCIAMethod);
		methodViewer = new ImpactMethodViewer(composite);
		methodViewer.setNullable(true);
		methodViewer.setInput(Database.get());
		UI.formLabel(composite, toolkit, Messages.NormalizationAndWeightingSet);
		nwViewer = new NwSetComboViewer(composite);
		nwViewer.setNullable(true);
		nwViewer.setDatabase(Database.get());
	}

	private void addListeners() {
		methodViewer.addSelectionChangedListener(
				(selection) -> handleMethodChange(selection));
		nwViewer.addSelectionChangedListener((selection) -> {
			Project project = editor.getModel();
			if (selection == null) {
				project.setNwSetId(null);
			} else {
				project.setNwSetId(selection.getId());
			}
			editor.setDirty(true);
		});
	}

	private void handleMethodChange(ImpactMethodDescriptor selection) {
		Project project = editor.getModel();
		if (selection == null && project.getImpactMethodId() == null)
			return;
		if (selection != null
				&& Objects.equals(selection.getId(),
						project.getImpactMethodId()))
			return;
		project.setNwSetId(null);
		if (selection == null)
			project.setImpactMethodId(null);
		else
			project.setImpactMethodId(selection.getId());
		project.setNwSetId(null);
		nwViewer.select(null);
		nwViewer.setInput(selection);
		if (indicatorTable != null)
			indicatorTable.methodChanged(selection);
		editor.setDirty(true);
	}

	private void setInitialSelection() {
		Project project = editor.getModel();
		if (project.getImpactMethodId() == null)
			return;
		IDatabase database = Database.get();
		ImpactMethodDao methodDao = new ImpactMethodDao(database);
		ImpactMethodDescriptor method = methodDao.getDescriptor(project
				.getImpactMethodId());
		if (method == null)
			return;
		methodViewer.select(method);
		NwSetDao dao = new NwSetDao(database);
		List<NwSetDescriptor> nwSets = dao.getDescriptorsForMethod(
				method.getId());
		nwViewer.setInput(nwSets);
		nwViewer.select(getInitialNwSet(project, nwSets));
	}

	private NwSetDescriptor getInitialNwSet(Project project,
			List<NwSetDescriptor> nwSets) {
		if (project.getNwSetId() == null)
			return null;
		for (NwSetDescriptor d : nwSets)
			if (project.getNwSetId() == d.getId())
				return d;
		return null;
	}

}
