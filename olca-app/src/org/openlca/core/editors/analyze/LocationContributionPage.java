package org.openlca.core.editors.analyze;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.FlowImpactSelection;
import org.openlca.core.editors.FlowImpactSelection.EventHandler;
import org.openlca.core.editors.HtmlView;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.Contribution;
import org.openlca.core.model.results.ContributionSet;
import org.openlca.core.model.results.LocationContribution;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.HtmlPage;
import org.openlca.ui.UI;
import org.openlca.ui.html.IHtmlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationContributionPage extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private AnalysisResult result;
	private Browser browser;
	private AnalyzeEditor editor;
	private LocationContributionTable table;
	private LocationContribution calculator;
	private FlowImpactSelection flowImpactSelection;

	public LocationContributionPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, "analysis.MapPage", Messages.Common_Locations);
		this.result = result;
		this.editor = editor;
		calculator = new LocationContribution(result,
				Messages.Common_Unspecified);
	}

	@Override
	public IHtmlResource getResource() {
		return HtmlView.GMAP_HEATMAP.getResource();
	}

	@Override
	public void onLoaded() {
		Flow[] flows = result.getFlowIndex().getFlows();
		if (flows != null && flows.length > 0) {
			Flow flow = flows[0];
			flowImpactSelection.selectWithEvent(flow);
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.Common_Locations);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createCombos(body, toolkit);
		createTable(body, toolkit);
		createBrowser(body, toolkit);
		form.reflow(true);
	}

	private void createCombos(Composite body, FormToolkit toolkit) {
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = FlowImpactSelection
				.onDatabase(editor.getDatabase()).withAnalysisResult(result)
				.withEventHandler(new SelectionHandler())
				.withSelection(result.getFlows()[0]).create(composite, toolkit);
	}

	private void createTable(Composite body, FormToolkit toolkit) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Common_ResultContributions);
		UI.gridLayout(composite, 1);
		table = new LocationContributionTable(composite);
	}

	private void createBrowser(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, Messages.Common_Map
				+ " (beta)");
		UI.bindActions(section, new RefreshMapAction());
		GridData gridData = UI.gridData(section, true, false);
		gridData.widthHint = 800;
		gridData.heightHint = 500;
		Composite browserComp = UI.sectionClient(section, toolkit);
		browserComp.setLayout(new FillLayout());
		browser = UI.createBrowser(browserComp, this);
	}

	private void renderMap(List<Contribution<Location>> contributions) {
		List<HeatmapPoint> points = new ArrayList<>();
		for (Contribution<Location> contribution : contributions) {
			if (!showContribution(contribution))
				continue;
			Location location = contribution.getItem();
			HeatmapPoint point = new HeatmapPoint();
			point.latitude = location.getLatitude();
			point.longitude = location.getLongitude();
			point.weight = (int) (100d * contribution.getShare());
			points.add(point);
		}
		if (points.size() == 1)
			points.get(0).weight = 1;
		String json = new Gson().toJson(points);
		log.trace("set map data: {}", json);
		browser.execute("setData(" + json + ")");
	}

	private boolean showContribution(Contribution<Location> contribution) {
		Location location = contribution.getItem();
		if (location == null)
			return false;
		if (location.getLatitude() == 0 && location.getLongitude() == 0)
			return false;
		if (contribution.getShare() <= 0)
			return false;
		return true;
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(Flow flow) {
			if (table == null || calculator == null || flow == null)
				return;
			ContributionSet<Location> set = calculator.calculate(flow);
			table.setInput(set.getContributions());
			renderMap(set.getContributions());
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			if (table == null || calculator == null || impactCategory == null)
				return;
			ContributionSet<Location> set = calculator
					.calculate(impactCategory);
			table.setInput(set.getContributions());
			renderMap(set.getContributions());
		}
	}

	private class RefreshMapAction extends Action {

		public RefreshMapAction() {
			setToolTipText(Messages.Reload);
			setImageDescriptor(ImageType.REFRESH_ICON.getDescriptor());
		}

		@Override
		public void run() {
			// force data binding
			flowImpactSelection.selectWithEvent(flowImpactSelection
					.getSelection());
		}

	}

	@SuppressWarnings("unused")
	private class HeatmapPoint {
		double latitude;
		double longitude;
		int weight;
	}
}
