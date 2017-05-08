package org.openlca.app.editors.processes;

import java.util.Collections;
import java.util.List;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.DQSystemDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.descriptors.DQSystemDescriptor;
import org.openlca.util.Strings;

public class DQSystemViewer extends AbstractComboViewer<DQSystemDescriptor> {

	public DQSystemViewer(Composite parent) {
		super(parent);
		setInput(new DQSystemDescriptor[0]);
	}

	public void setInput(IDatabase database) {
		List<DQSystemDescriptor> systems = new DQSystemDao(database).getDescriptors();
		Collections.sort(systems, (sys1, sys2) -> {
			if (sys1 == null || sys2 == null)
				return 0;
			return Strings.compare(sys1.getName(), sys2.getName());
		});
		super.setInput(systems);
	}

	@Override
	public TableComboViewer getViewer() {
		return super.getViewer();
	}

	@Override
	public Class<DQSystemDescriptor> getType() {
		return DQSystemDescriptor.class;
	}

}
