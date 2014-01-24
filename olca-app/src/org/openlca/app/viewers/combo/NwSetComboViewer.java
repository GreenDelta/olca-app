package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;

public class NwSetComboViewer extends AbstractComboViewer<NwSetDescriptor> {

	private IDatabase database;

	public NwSetComboViewer(Composite parent) {
		super(parent);
		setInput(new NwSetDescriptor[0]);
	}

	public void setDatabase(IDatabase database) {
		this.database = database;
	}

	public void setInput(ImpactMethodDescriptor impactMethod) {
		if (database == null)
			throw new IllegalStateException("No database set");
		if (impactMethod != null) {
			NwSetDao dao = new NwSetDao(database);
			List<NwSetDescriptor> nwSets = dao
					.getDescriptorsForMethod(impactMethod.getId());
			setInput(nwSets.toArray(new NwSetDescriptor[nwSets.size()]));
		} else {
			setInput(new NwSetDescriptor[0]);
		}
	}

	public NwSetDescriptor find(long id) {
		for (NwSetDescriptor nwSet : getInput())
			if (nwSet.getId() == id)
				return nwSet;
		return null;
	}

	@Override
	public Class<NwSetDescriptor> getType() {
		return NwSetDescriptor.class;
	}

}
