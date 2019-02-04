package org.openlca.app.viewers.combo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.Location;
import org.openlca.util.Strings;

public class LocationViewer extends AbstractComboViewer<Location> {

	public LocationViewer(Composite parent) {
		super(parent);
		setInput(new Location[0]);
	}

	public void setInput(IDatabase database) {
		List<Location> locations = new LocationDao(database).getAll();
		Collections.sort(locations, new Comparator<Location>() {
			@Override
			public int compare(Location loc1, Location loc2) {
				if (loc1 == null || loc2 == null)
					return 0;
				return Strings.compare(loc1.name, loc2.name);
			}
		});
		super.setInput(locations.toArray(new Location[locations.size()]));
	}

	@Override
	public Class<Location> getType() {
		return Location.class;
	}

}
