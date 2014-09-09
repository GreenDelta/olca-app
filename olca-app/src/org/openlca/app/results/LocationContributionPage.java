package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.LocationContribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationContributionPage extends FormPage implements HtmlPage {

	private EntityCache cache = Cache.getEntityCache();
	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionResultProvider<?> result;
	private Browser browser;
	private LocationContributionTable table;
	private LocationContribution calculator;
	private FlowImpactSelection flowImpactSelection;

	public LocationContributionPage(FormEditor editor,
			ContributionResultProvider<?> result) {
		super(editor, "analysis.MapPage", Messages.Locations);
		this.result = result;
		calculator = new LocationContribution(result);
	}

	@Override
	public String getUrl() {
		return HtmlView.GMAP_HEATMAP.getUrl();
	}

	@Override
	public void onLoaded() {
		Set<FlowDescriptor> flows = result.getFlowDescriptors();
		if (flows.size() > 0) {
			FlowDescriptor flow = flows.iterator().next();
			flowImpactSelection.selectWithEvent(flow);
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Locations);
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
		flowImpactSelection = FlowImpactSelection.on(result, cache)
				.withEventHandler(new SelectionHandler())
				.withSelection(result.getFlowDescriptors().iterator().next())
				.create(composite, toolkit);
	}

	private void createTable(Composite body, FormToolkit toolkit) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.ResultContributions);
		UI.gridLayout(composite, 1);
		table = new LocationContributionTable(composite);
	}

	private void createBrowser(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, Messages.Map + " (beta)");
		Actions.bind(section, new RefreshMapAction());
		GridData gridData = UI.gridData(section, true, false);
		gridData.widthHint = 800;
		gridData.heightHint = 500;
		Composite browserComp = UI.sectionClient(section, toolkit);
		browserComp.setLayout(new FillLayout());
		browser = UI.createBrowser(browserComp, this);
	}

	private void renderMap(List<ContributionItem<Location>> contributions) {
		List<HeatmapPoint> points = new ArrayList<>();
		for (ContributionItem<Location> contribution : contributions) {
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

	private boolean showContribution(ContributionItem<Location> contribution) {
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
		public void flowSelected(FlowDescriptor flow) {
			if (table == null || calculator == null || flow == null)
				return;
			String unit = Labels.getRefUnit(flow, result.getCache());
			ContributionSet<Location> set = calculator.calculate(flow);
			setData(unit, set);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			if (table == null || calculator == null || impact == null)
				return;
			String unit = impact.getReferenceUnit();
			ContributionSet<Location> set = calculator.calculate(impact);
			setData(unit, set);
		}

		private void setData(String unit, ContributionSet<Location> set) {
			List<ContributionItem<Location>> items = set.getContributions();
			Contributions.sortDescending(items);
			table.setInput(items, unit);
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
