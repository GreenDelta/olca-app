package org.openlca.app.results.contributions.locations;

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
import org.openlca.app.M;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.CurrencyDao;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Currency;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.BaseDescriptor;
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
public class LocationPage extends FormPage implements HtmlPage {

	private EntityCache cache = Cache.getEntityCache();
	private Logger log = LoggerFactory.getLogger(getClass());
	private ContributionResultProvider<?> result;
	private Browser browser;
	private LocationTree tree;
	private LocationContribution calculator;
	private ResultTypeSelection combos;
	private TreeConentBuilder inputBuilder;
	private boolean showMap;

	public LocationPage(FormEditor editor,
			ContributionResultProvider<?> result) {
		this(editor, result, true);
	}

	public LocationPage(FormEditor editor,
			ContributionResultProvider<?> result, boolean showMap) {
		super(editor, "analysis.MapPage", M.Locations);
		this.showMap = showMap;
		this.result = result;
		this.inputBuilder = new TreeConentBuilder(result);
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
			combos.selectWithEvent(flow);
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, M.Locations);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createCombos(body, toolkit);
		createTree(body, toolkit);
		if (showMap)
			createBrowser(body, toolkit);
		if (result.getFlowDescriptors().size() > 0)
			combos.selectWithEvent(result.getFlowDescriptors().iterator()
					.next());
		form.reflow(true);
	}

	private void createCombos(Composite body, FormToolkit toolkit) {
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		combos = ResultTypeSelection.on(result, cache)
				.withEventHandler(new SelectionHandler())
				.withSelection(result.getFlowDescriptors().iterator().next())
				.create(composite, toolkit);
	}

	private void createTree(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, M.ContributionTreeLocations);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, tk);
		UI.gridLayout(composite, 1);
		tree = new LocationTree(composite, showMap);
	}

	private void createBrowser(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, M.Map + " (beta)");
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
			Location location = contribution.item;
			HeatmapPoint point = new HeatmapPoint();
			point.latitude = location.getLatitude();
			point.longitude = location.getLongitude();
			point.weight = (int) (100d * contribution.share);
			points.add(point);
		}
		if (points.size() == 1)
			points.get(0).weight = 1;
		String json = new Gson().toJson(points);
		log.trace("set map data: {}", json);
		browser.execute("setData(" + json + ")");
	}

	private boolean showContribution(ContributionItem<Location> contribution) {
		Location location = contribution.item;
		if (location == null)
			return false;
		if (location.getLatitude() == 0 && location.getLongitude() == 0)
			return false;
		if (contribution.share <= 0)
			return false;
		return true;
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(FlowDescriptor flow) {
			if (tree == null || calculator == null || flow == null)
				return;
			String unit = Labels.getRefUnit(flow, result.cache);
			ContributionSet<Location> set = calculator.calculate(flow);
			double total = result.getTotalFlowResult(flow).value;
			setData(set, flow, total, unit);
		}

		@Override
		public void impactCategorySelected(ImpactCategoryDescriptor impact) {
			if (tree == null || calculator == null || impact == null)
				return;
			String unit = impact.getReferenceUnit();
			ContributionSet<Location> set = calculator.calculate(impact);
			double total = result.getTotalImpactResult(impact).value;
			setData(set, impact, total, unit);
		}

		@Override
		public void costResultSelected(CostResultDescriptor cost) {
			if (tree == null || calculator == null || cost == null)
				return;
			String unit = getCurrency();
			if (cost.forAddedValue) {
				ContributionSet<Location> set = calculator.addedValues();
				double total = result.getTotalCostResult();
				total = total == 0 ? 0 : -total;
				setData(set, cost, total, unit);
			} else {
				ContributionSet<Location> set = calculator.netCosts();
				double total = result.getTotalCostResult();
				setData(set, cost, total, unit);
			}
		}

		private String getCurrency() {
			try {
				CurrencyDao dao = new CurrencyDao(Database.get());
				Currency ref = dao.getReferenceCurrency();
				if (ref == null)
					return "?";
				else
					return ref.code != null ? ref.code : ref.getName();
			} catch (Exception e) {
				log.error("failed to get reference currency", e);
				return "?";
			}
		}

		private void setData(ContributionSet<Location> set,
				BaseDescriptor selection, double total, String unit) {
			Contributions.sortDescending(set.contributions);
			List<LocationItem> items = inputBuilder.build(set, selection, total);
			tree.setInput(items, unit);
			if (showMap)
				renderMap(set.contributions);
		}

	}

	private class RefreshMapAction extends Action {

		public RefreshMapAction() {
			setToolTipText(M.Reload);
			setImageDescriptor(Icon.REFRESH.descriptor());
		}

		@Override
		public void run() {
			// force data binding
			combos.selectWithEvent(combos.getSelection());
		}
	}

	@SuppressWarnings("unused")
	private class HeatmapPoint {
		double latitude;
		double longitude;
		int weight;
	}
}
