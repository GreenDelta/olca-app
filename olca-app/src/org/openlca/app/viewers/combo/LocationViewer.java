package org.openlca.app.viewers.combo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Location;
import org.openlca.util.Strings;

public class LocationViewer extends AbstractComboViewer<Location> {

	public static final int NULLABLE = 1;
	private boolean nullable;

	public LocationViewer(Composite parent) {
		this(parent, -1);
	}

	public LocationViewer(Composite parent, int flag) {
		super(parent);
		nullable = flag == NULLABLE;
		setInput(new Location[0]);
	}

	public void setInput(IDatabase database) {
		try {
			List<Location> locations = database.createDao(Location.class)
					.getAll();
			Collections.sort(locations, new Comparator<Location>() {
				@Override
				public int compare(Location loc1, Location loc2) {
					if (loc1 == null || loc2 == null)
						return 0;
					return Strings.compare(loc1.getName(), loc2.getName());
				}
			});
			if (nullable) {
				Location[] values = new Location[locations.size() + 1];
				for (int i = 0; i < locations.size(); i++)
					values[i + 1] = locations.get(i);
				setInput(values);
				select(null);
			} else
				setInput(locations.toArray(new Location[locations.size()]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Class<Location> getType() {
		return Location.class;
	}

}
