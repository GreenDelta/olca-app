package org.openlca.app.results.viz;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.results.viz.BubbleChartDataSet.Item;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContributionBubblePage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionResultProvider<?> result;
	private Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public ContributionBubblePage(FormEditor editor,
			ContributionResultProvider<?> result) {
		super(editor, "ContributionBubblePage",
				"Process contributions - Bubble Chart");
		this.result = result;
	}

	@Override
	public String getUrl() {
		return HtmlView.BUBBLE_CHART.getUrl();
	}

	@Override
	public void onLoaded() {
		FlowIndex flowIndex = result.getResult().getFlowIndex();
		long flowId = flowIndex.getFlowAt(0);
		FlowDescriptor first = Cache.getEntityCache().get(FlowDescriptor.class,
				flowId);
		if (first == null)
			return;
		flowImpactSelection.selectWithEvent(first);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Process contributions");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = FlowImpactSelection
				.on(result, Cache.getEntityCache())
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(FlowDescriptor flow) {
			ContributionSet<ProcessDescriptor> set = result
					.getProcessContributions(flow);
			BubbleChartDataSet dataSet = new BubbleChartDataSet();
			dataSet.setRefName(Labels.getDisplayName(flow));
			dataSet.setRefUnit(Labels.getRefUnit(flow, result.getCache()));
			dataSet.setTotalAmount(result.getTotalFlowResult(flow).getValue());
			setItems(set, dataSet);
			setResultData(dataSet);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			ContributionSet<ProcessDescriptor> set = result
					.getProcessContributions(impact);
			BubbleChartDataSet dataSet = new BubbleChartDataSet();
			dataSet.setRefName(Labels.getDisplayName(impact));
			dataSet.setRefUnit(impact.getReferenceUnit());
			dataSet.setTotalAmount(result.getTotalImpactResult(impact)
					.getValue());
			setItems(set, dataSet);
			setResultData(dataSet);
		}

		private void setItems(ContributionSet<ProcessDescriptor> set,
				BubbleChartDataSet dataSet) {
			for (ContributionItem<ProcessDescriptor> item : set
					.getContributions()) {
				Item bubbleItem = new Item();
				bubbleItem.setAmount(item.getAmount());
				bubbleItem.setName(Labels.getDisplayName(item.getItem()));
				dataSet.getItems().add(bubbleItem);
			}
		}

		private void setResultData(BubbleChartDataSet dataSet) {
			String command = "setData(" + dataSet.toJson() + ")";
			try {
				log.trace("set bubble result data for {}", dataSet.getRefName());
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to set bubble chart data", e);
			}
		}
	}

}
