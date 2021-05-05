package org.openlca.app.editors.projects;

import java.util.ArrayList;

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
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;

class ImpactSection {

	private final ProjectEditor editor;
	private final IDatabase db;

	private ImpactMethodViewer methodCombo;
	private NwSetComboViewer nwSetCombo;

	public ImpactSection(ProjectEditor editor) {
		this.editor = editor;
		this.db = Database.get();
	}

	public void render(Composite body, FormToolkit tk) {
		var rootComp = UI.formSection(body, tk, M.LCIAMethod, 1);
		var formComp = UI.formComposite(rootComp, tk);
		UI.gridLayout(formComp, 3);
		UI.gridData(formComp, true, false);
		createViewers(tk, formComp);
		setInitialSelection();
		addListeners();
	}

	private void createViewers(FormToolkit tk, Composite comp) {
		UI.formLabel(comp, tk, M.LCIAMethod);
		methodCombo = new ImpactMethodViewer(comp);
		methodCombo.setNullable(true);
		methodCombo.setInput(Database.get());
		new CommentControl(comp, tk, "impactMethod", editor.getComments());
		UI.formLabel(comp, tk, M.NormalizationAndWeightingSet);
		nwSetCombo = new NwSetComboViewer(comp, Database.get());
		nwSetCombo.setNullable(true);
		new CommentControl(comp, tk, "nwSet", editor.getComments());
	}

	private void addListeners() {
		methodCombo.addSelectionChangedListener(this::onMethodChange);
		nwSetCombo.addSelectionChangedListener(d -> {
			var project = editor.getModel();
			project.nwSet = d == null
					? null
					: new NwSetDao(db).getForId(d.id);
			editor.setDirty(true);
		});
	}

	private void onMethodChange(ImpactMethodDescriptor method) {
		Project project = editor.getModel();

		// first check if something changed
		if (method == null && project.impactMethod == null)
			return;
		if (method != null
				&& project.impactMethod != null
				&& method.id == project.impactMethod.id)
			return;

		// handle change
		project.impactMethod = method == null
				? null
				: new ImpactMethodDao(db).getForId(method.id);
		project.nwSet = null;
		nwSetCombo.select(null);
		nwSetCombo.setInput(method);
		editor.setDirty(true);
	}

	private void setInitialSelection() {
		Project project = editor.getModel();
		if (project.impactMethod == null)
			return;
		methodCombo.select(Descriptor.of(project.impactMethod));

		// normalisation and weighting sets
		var nws = new ArrayList<NwSetDescriptor>();
		NwSetDescriptor selected = null;
		for (var nwSet : project.impactMethod.nwSets) {
			var d = Descriptor.of(nwSet);
			nws.add(d);
			if (project.nwSet != null
				&& project.nwSet.id == nwSet.id) {
				selected = d;
			}
		}
		nwSetCombo.setInput(nws);
		nwSetCombo.select(selected);
	}
}
