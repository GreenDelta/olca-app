package org.openlca.app.editors.processes;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.DQSystem;
import org.openlca.util.Strings;

public class DQSystemViewer extends AbstractComboViewer<DQSystem> {

	public DQSystemViewer(Composite parent) {
		super(parent);
		setInput(new DQSystem[0]);
	}

	public void setInput(IDatabase database) {
		List<DQSystem> systems = database.createDao(DQSystem.class).getAll();
		Collections.sort(systems, new Comparator<DQSystem>() {
			@Override
			public int compare(DQSystem sys1, DQSystem sys2) {
				if (sys1 == null || sys2 == null)
					return 0;
				return Strings.compare(sys1.getName(), sys2.getName());
			}
		});
		super.setInput(systems.toArray(new DQSystem[systems.size()]));
	}

	@Override
	public Class<DQSystem> getType() {
		return DQSystem.class;
	}

}
