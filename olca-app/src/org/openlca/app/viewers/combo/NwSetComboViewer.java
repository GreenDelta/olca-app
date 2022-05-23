package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.model.NwSet;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.Strings;

public class NwSetComboViewer extends AbstractComboViewer<NwSet> {

	private final IDatabase database;

	public NwSetComboViewer(Composite parent, IDatabase db) {
		super(parent);
		setInput(new NwSet[0]);
		this.database = db;
	}

	public void setInput(ImpactMethodDescriptor method) {
		if (database == null)
			throw new IllegalStateException("No database set");
		if (method == null) {
			setInput(new NwSet[0]);
		} else {
			var nwSets = new NwSetDao(database)
					.allOfMethod(method.id)
					.stream()
					.sorted((n1, n2) -> Strings.compare(n1.name, n2.name))
					.toArray(NwSet[]::new);
			setInput(nwSets);
		}
	}

	@Override
	public Class<NwSet> getType() {
		return NwSet.class;
	}

}
