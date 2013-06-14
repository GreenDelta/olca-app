package org.openlca.core.application.db;

import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.application.views.search.SearchView;

/** Refresh of the navigation: must be executed in UI */
class NavigationCallback implements Runnable {
	@Override
	public void run() {
		Navigator.refresh(2);
		SearchView.refresh();
	}
}