package org.openlca.app.projects;

import java.util.Set;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.ProjectResult;

public class ProjectResultPage extends FormPage {

	private EntityCache cache = Database.getCache();
	private ProjectResult result;
	private ProjectResultChart chart;
	private FlowImpactSelection selector;
	private TableViewer tableViewer;

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
		selector = FlowImpactSelection.on(result, cache)
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		createTable(body, toolkit);
		createChart(body);
		toolkit.adapt(chart);
		initialSelection(selector);
		form.reflow(true);
	}

	private void createTable(Composite body, FormToolkit toolkit) {
		Composite composite = UI.formSection(body, toolkit, "Results");
		UI.gridLayout(composite, 1);
		tableViewer = Tables.createViewer(composite, new String[] { "Variant",
				"Amount", "Unit" });
		tableViewer.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(tableViewer, 0.4, 0.3, 0.3);
		UI.gridData(tableViewer.getTable(), true, true).minimumHeight = 150;
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
		gridData.heightHint = 350;
		gridData.widthHint = 550;
	}

	private class SelectionHandler implements EventHandler {
		@Override
		public void flowSelected(FlowDescriptor flow) {
			ContributionSet<ProjectVariant> contributionSet = result
					.getContributions(flow);
			chart.renderChart(flow, contributionSet);
			tableViewer.setInput(contributionSet.getContributions());
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			ContributionSet<ProjectVariant> contributionSet = result
					.getContributions(impactCategory);
			chart.renderChart(impactCategory, contributionSet);
			tableViewer.setInput(contributionSet.getContributions());
		}
	}

	private class TableLabel extends LabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof Contribution) || columnIndex > 0)
				return null;
			Contribution<?> contribution = (Contribution<?>) element;
			return image.getForTable(contribution.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int col) {
			if (!(element instanceof Contribution))
				return null;
			Contribution<ProjectVariant> contribution = (Contribution<ProjectVariant>) element;
			switch (col) {
			case 0:
				return contribution.getItem().getName();
			case 1:
				return Numbers.format(contribution.getAmount());
			case 3:
				return getUnit();
			default:
				return null;
			}
		}

		private String getUnit() {
			Object selection = selector.getSelection();
			if (selection instanceof FlowDescriptor)
				return Labels.getRefUnit((FlowDescriptor) selection,
						Database.getCache());
			if (selection instanceof ImpactCategoryDescriptor)
				return ((ImpactCategoryDescriptor) selection)
						.getReferenceUnit();
			return null;
		}

	}

}
