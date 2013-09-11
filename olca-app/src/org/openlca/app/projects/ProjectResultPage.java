package org.openlca.app.projects;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.editors.FlowImpactSelection;
import org.openlca.core.editors.FlowImpactSelection.EventHandler;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.ProjectResult;

public class ProjectResultPage extends FormPage {

	private EntityCache cache = Database.getCache();
	private ProjectResult result;
	private ProjectResultChart chart;

	public ProjectResultPage(ProjectResultEditor editor) {
		super(editor, "ProjectResultPage", "Project results");
		this.result = editor.getResult();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Project results");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		FlowImpactSelection selector = FlowImpactSelection.on(result, cache)
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		createChart(body);
		toolkit.adapt(chart);
		initialSelection(selector);
		form.reflow(true);
	}

	private void initialSelection(FlowImpactSelection selector) {
		Set<FlowDescriptor> flowSet = result.getFlows(cache);
		if (flowSet.isEmpty())
			return;
		FlowDescriptor flow = flowSet.iterator().next();
		selector.selectWithEvent(flow);
	}

	private void createChart(Composite body) {
		chart = new ProjectResultChart(body);
		GridData gridData = UI.gridData(chart, false, false);
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.heightHint = 450;
		gridData.widthHint = 650;
	}

	private class SelectionHandler implements EventHandler {
		@Override
		public void flowSelected(FlowDescriptor flow) {
			ContributionSet<ProjectVariant> contributionSet = result
					.getContributions(flow);
			chart.renderChart(flow, contributionSet);
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			ContributionSet<ProjectVariant> contributionSet = result
					.getContributions(impactCategory);
			chart.renderChart(impactCategory, contributionSet);
		}
	}

}
