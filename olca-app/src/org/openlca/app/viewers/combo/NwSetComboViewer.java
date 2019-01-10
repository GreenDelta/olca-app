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

	public void setInput(ImpactMethodDescriptor method) {
		if (database == null)
			throw new IllegalStateException("No database set");
		if (method != null) {
			NwSetDao dao = new NwSetDao(database);
			List<NwSetDescriptor> nwSets = dao
					.getDescriptorsForMethod(method.id);
			setInput(nwSets.toArray(new NwSetDescriptor[nwSets.size()]));
		} else {
			setInput(new NwSetDescriptor[0]);
		}
	}

	@Override
	public Class<NwSetDescriptor> getType() {
		return NwSetDescriptor.class;
	}

}
