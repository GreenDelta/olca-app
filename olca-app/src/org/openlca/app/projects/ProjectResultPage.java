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
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.FlowDescriptor;
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
		FlowViewer viewer = createFlowViewer(body);
		createChart(body);
		toolkit.adapt(chart);
		setResultsFor(viewer.getSelected());
	}

	private FlowViewer createFlowViewer(Composite body) {
		FlowViewer viewer = new FlowViewer(body, Database.getCache());
		Set<FlowDescriptor> flowSet = result.getFlows(cache);
		FlowDescriptor[] flows = flowSet.toArray(new FlowDescriptor[flowSet
				.size()]);
		viewer.setInput(flows);
		viewer.selectFirst();
		viewer.addSelectionChangedListener(new ISelectionChangedListener<FlowDescriptor>() {
			@Override
			public void selectionChanged(FlowDescriptor selection) {
				setResultsFor(selection);
			}
		});
		return viewer;
	}

	private void createChart(Composite body) {
		chart = new ProjectResultChart(body);
		GridData gridData = UI.gridData(chart, false, false);
		gridData.horizontalAlignment = SWT.CENTER;
		gridData.heightHint = 450;
		gridData.widthHint = 650;
	}

	private void setResultsFor(FlowDescriptor flow) {
		ContributionSet<ProjectVariant> contributionSet = result
				.getContributions(flow);
		chart.renderChart(flow, contributionSet);
	}

}
