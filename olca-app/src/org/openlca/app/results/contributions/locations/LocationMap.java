package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.results.ContributionItem;

import com.google.gson.Gson;

import javafx.scene.web.WebEngine;

class LocationMap implements WebPage {

	private LocationPage page;
	private WebEngine webkit;

	static LocationMap create(LocationPage page, Composite body, FormToolkit tk) {
		return new LocationMap(page, body, tk);
	}

	LocationMap(LocationPage page, Composite body, FormToolkit tk) {
		this.page = page;
		Section section = UI.section(body, tk, M.Map + " (beta)");
		Actions.bind(section, new RefreshMapAction());
		GridData gridData = UI.gridData(section, true, false);
		gridData.widthHint = 800;
		gridData.heightHint = 500;
		Composite browserComp = UI.sectionClient(section, tk);
		browserComp.setLayout(new FillLayout());
		UI.createWebView(browserComp, this);
	}

	@Override
	public String getUrl() {
		return HtmlView.GMAP_HEATMAP.getUrl();
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		page.refreshSelection();
	}

	void setInput(List<LocationItem> items) {
		List<HeatmapPoint> points = new ArrayList<>();
		for (LocationItem item : items) {
			ContributionItem<Location> ci = item.contribution;
			if (!showInMap(ci))
				continue;
			Location location = ci.item;
			HeatmapPoint point = new HeatmapPoint();
			point.latitude = location.getLatitude();
			point.longitude = location.getLongitude();
			point.weight = (int) (100d * ci.share);
			points.add(point);
		}
		if (points.size() == 1) {
			points.get(0).weight = 1;
		}
		String json = new Gson().toJson(points);
		webkit.executeScript("setData(" + json + ")");
	}

	private boolean showInMap(ContributionItem<Location> ci) {
		if (ci == null)
			return false;
		Location location = ci.item;
		if (location == null)
			return false;
		if (location.getLatitude() == 0 && location.getLongitude() == 0)
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
