package org.openlca.app.editors.projects;

import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NwSetComboViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

class CalculationSetupSection {

	private final ProjectEditor editor;
	private final IDatabase db;

	private ImpactMethodViewer methodCombo;
	private NwSetComboViewer nwSetCombo;

	public CalculationSetupSection(ProjectEditor editor) {
		this.editor = editor;
		this.db = Database.get();
	}

	public void render(Composite body, FormToolkit tk) {
		var rootComp = UI.formSection(body, tk, M.CalculationSetup, 1);
		var formComp = UI.composite(rootComp, tk);
		UI.gridLayout(formComp, 3);
		UI.gridData(formComp, true, false);
		createViewers(tk, formComp);
		setInitialSelection();
		addListeners();
	}

	private void createViewers(FormToolkit tk, Composite comp) {
		UI.label(comp, tk, M.ImpactAssessmentMethod);

		// impact method
		methodCombo = new ImpactMethodViewer(comp);
		methodCombo.setNullable(true);
		methodCombo.setInput(Database.get());
		new CommentControl(comp, tk, "impactMethod", editor.getComments());

		// NW set
		UI.label(comp, tk, M.NormalizationAndWeightingSet);
		nwSetCombo = new NwSetComboViewer(comp, Database.get());
		nwSetCombo.setNullable(true);
		new CommentControl(comp, tk, "nwSet", editor.getComments());

		// regionalized calculation
		UI.filler(comp, tk);
		var regioCheck = UI.checkbox(comp, tk);
		regioCheck.setText(M.RegionalizedLCIA);
		regioCheck.setSelection(editor.getModel().isWithRegionalization);
		Controls.onSelect(regioCheck, $ -> {
			var project = editor.getModel();
			project.isWithRegionalization = !project.isWithRegionalization;
			editor.setDirty(true);
		});
		UI.filler(comp, tk);

		// LCC calculation
		UI.filler(comp, tk);
		var costsCheck = UI.checkbox(comp, tk);
		costsCheck.setText(M.IncludeCostCalculation);
		costsCheck.setSelection(editor.getModel().isWithCosts);
		Controls.onSelect(costsCheck, $ -> {
			var project = editor.getModel();
			project.isWithCosts = !project.isWithCosts;
			editor.setDirty(true);
		});
		UI.filler(comp, tk);

		// report button
		if (editor.report == null) {
			var beforeReport = UI.filler(comp, tk);
			var reportBtn = UI.button(comp, tk, M.CreateReport);
			var afterButton = UI.filler(comp, tk);
			reportBtn.setImage(Images.get(ModelType.PROJECT));
			Controls.onSelect(reportBtn, $ -> {
				try {

					// create the report and add the editor page
					editor.report = Report.initDefault();
					var reportPage = new ReportEditorPage(editor);
					editor.addPage(reportPage);
					editor.setActivePage(reportPage.getId());
					editor.setDirty(true);
					applyTheme();

					// dispose the button row and repaint the form
					afterButton.dispose();
					reportBtn.dispose();
					beforeReport.dispose();
					var c = comp;
					while (true) {
						c = c.getParent();
						if (c == null)
							break;
						if (c instanceof ScrolledForm) {
							((ScrolledForm) c).reflow(true);
							break;
						}
					}
				} catch (PartInitException e) {
					ErrorReporter.on("Failed to add report editor", e);
				}
			});
		}
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
		var nws = new ArrayList<NwSet>();
		NwSet selected = null;
		for (var nwSet : project.impactMethod.nwSets) {
			nws.add(nwSet);
			if (project.nwSet != null
					&& project.nwSet.id == nwSet.id) {
				selected = nwSet;
			}
		}
		nwSetCombo.setInput(nws);
		nwSetCombo.select(selected);
	}

	private void applyTheme() {
		// on Windows, when we are in dark mode and add
		// the report page, the background of that page
		// is still white. to apply the dark theme, a
		// hack is to set the focus to the navigation
		// and then bring it back
		var page = Editors.getActivePage();
		if (page == null)
			return;
		var navi = page.findView("views.navigation");
		if (navi != null) {
			navi.setFocus();
		}
		editor.setFocus();
	}
}
