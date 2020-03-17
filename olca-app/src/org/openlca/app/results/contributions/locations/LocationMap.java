package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.results.Contribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * @deprecated we now use the {@link ResultMap} for visualizing the contribution
 *             results of the locations.
 */
@Deprecated
class LocationMap {

	private LocationPage page;
	private Browser browser;

	static LocationMap create(LocationPage page, Composite body, FormToolkit tk) {
		return new LocationMap(page, body, tk);
	}

	LocationMap(LocationPage page, Composite body, FormToolkit tk) {
		this.page = page;
		Section section = UI.section(body, tk, M.Map);
		Actions.bind(section, new RefreshMapAction());
		GridData gridData = UI.gridData(section, true, false);
		gridData.widthHint = 800;
		gridData.heightHint = 500;
		Composite browserComp = UI.sectionClient(section, tk);
		browserComp.setLayout(new FillLayout());
		browser = new Browser(browserComp, SWT.NONE);
		browser.setJavascriptEnabled(true);
		UI.onLoaded(browser, HtmlFolder.getUrl("location_heatmap.html"), 
				() -> page.refreshSelection());
	}

	void setInput(List<Contribution<Location>> items) {
		if (browser == null)
			return;
		List<HeatmapPoint> points = new ArrayList<>();
		for (Contribution<Location> c : items) {
			if (!showInMap(c))
				continue;
			Location location = c.item;
			HeatmapPoint point = new HeatmapPoint();
			point.latitude = location.latitude;
			point.longitude = location.longitude;
			point.weight = (int) (100d * c.share);
			points.add(point);
		}
		if (points.size() == 1) {
			points.get(0).weight = 1;
		}
		String json = new Gson().toJson(points);
		try {
			browser.execute("setData(" + json + ")");
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(LocationMap.class);
			log.warn("Error setting location data", e);
		}
	}

	private boolean showInMap(Contribution<Location> ci) {
		if (ci == null)
			return false;
		Location location = ci.item;
		if (location == null)
			return false;
		if (location.latitude == 0 && location.longitude == 0)
			return false;
		if (ci.share <= 0)
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
			page.refreshSelection();
		}
	}

	@SuppressWarnings("unused")
	private class HeatmapPoint {
		double latitude;
		double longitude;
		int weight;
	}
}
