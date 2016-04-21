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
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Shows the contributions of the locations in the product system to an analysis
 * result.
 */
public class LocationPage extends FormPage implements HtmlPage {

	private EntityCache cache = Cache.getEntityCache();
	ContributionResultProvider<?> result;

	private Logger log = LoggerFactory.getLogger(getClass());
	private Browser browser;
	private ResultTypeSelection combos;

	LocationTree tree;
	boolean showMap;

	public LocationPage(FormEditor editor,
			ContributionResultProvider<?> result) {
		this(editor, result, true);
	}

	public LocationPage(FormEditor editor,
			ContributionResultProvider<?> result, boolean showMap) {
		super(editor, "analysis.MapPage", M.Locations);
		this.showMap = showMap;
		this.result = result;
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
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.Locations);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createCombos(body, tk);
		createTree(body, tk);
		if (showMap) {
			createBrowser(body, tk);
		}
		form.reflow(true);
	}

	private void createCombos(Composite body, FormToolkit toolkit) {
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		combos = ResultTypeSelection.on(result, cache)
				.withEventHandler(new SelectionHandler(this))
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

	void renderMap(List<ContributionItem<Location>> contributions) {
		List<HeatmapPoint> points = new ArrayList<>();
		for (ContributionItem<Location> contribution : contributions) {
			if (!showInMap(contribution))
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

	private boolean showInMap(ContributionItem<Location> contribution) {
		Location location = contribution.item;
		if (location == null)
			return false;
		if (location.getLatitude() == 0 && location.getLongitude() == 0)
			return false;
		if (contribution.share <= 0)
			return false;
		return true;
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
