package org.openlca.ui.viewer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Location;
import org.openlca.util.Strings;

public class LocationViewer extends AbstractComboViewer<Location> {

	public LocationViewer(Composite parent) {
		super(parent);
		setInput(new Location[0]);
	}

	public void setInput(IDatabase database) {
		try {
			final List<Location> locations = database.createDao(Location.class)
					.getAll();
			Collections.sort(locations, new Comparator<Location>() {

				@Override
				public int compare(Location arg0, Location arg1) {
					String name1 = arg0 != null ? arg0.getName().toLowerCase()
							: null;
					String name2 = arg1 != null ? arg1.getName().toLowerCase()
							: null;
					return Strings.compare(name1, name2);
				}

			});
			setInput(locations.toArray(new Location[locations.size()]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
