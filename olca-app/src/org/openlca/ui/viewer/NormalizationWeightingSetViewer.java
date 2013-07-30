package org.openlca.ui.viewer;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

public class NormalizationWeightingSetViewer extends
		AbstractComboViewer<NormalizationWeightingSet> {

	private IDatabase database;

	public NormalizationWeightingSetViewer(Composite parent) {
		super(parent);
		setInput(new NormalizationWeightingSet[0]);
	}

	public void setDatabase(IDatabase database) {
		this.database = database;
	}

	public void setInput(ImpactMethodDescriptor impactMethod) {
		if (database == null)
			throw new IllegalStateException("No database set");
		if (impactMethod != null) {
			ImpactMethodDao dao = new ImpactMethodDao(
					database);
			List<NormalizationWeightingSet> nwSets = dao
					.getNwSetDescriptors(impactMethod);
			setInput(nwSets
					.toArray(new NormalizationWeightingSet[nwSets.size()]));
		} else {
			setInput(new NormalizationWeightingSet[0]);
		}
	}

	public NormalizationWeightingSet find(String id) {
		for (NormalizationWeightingSet nwSet : getInput())
			if (id != null && id.equals(nwSet.getId()))
				return nwSet;
		return null;
	}
}
