package org.openlca.app.editors.processes.exchanges;

import java.util.Collections;
import java.util.Set;

import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ProviderCombo extends ComboBoxCellModifier<Exchange, ProcessDescriptor> {

	private final IDatabase db = Database.get();
	private final EntityCache cache = Cache.getEntityCache();
	private final ProcessEditor editor;

	ProviderCombo(ProcessEditor editor) {
		this.editor = editor;
	}

	@Override
	public boolean canModify(Exchange e) {
		if (e == null || e.flow == null)
			return false;
		var type = e.flow.flowType;
		return e.isInput
			? type == FlowType.PRODUCT_FLOW
			: type == FlowType.WASTE_FLOW;
	}

	@Override
	protected ProcessDescriptor[] getItems(Exchange e) {
		var providerIds = getProviderIds(e);
		if (providerIds.isEmpty())
			return new ProcessDescriptor[0];
		var providers = cache.getAll(ProcessDescriptor.class, providerIds)
			.values().stream()
			.sorted((p1, p2) -> Strings.compare(Labels.name(p1), Labels.name(p2)))
			.toList();
		var array = new ProcessDescriptor[providers.size() + 1];
		for (int i = 0; i < providers.size(); i++) {
			array[i + 1] = providers.get(i);
		}
		return array;
	}

	@Override
	protected ProcessDescriptor getItem(Exchange e) {
		return e.defaultProviderId != 0
			? cache.get(ProcessDescriptor.class, e.defaultProviderId)
			: null;
	}

	@Override
	protected String getText(ProcessDescriptor d) {
		return d != null
			? Labels.name(d)
			: "";
	}

	@Override
	protected void setItem(Exchange e, ProcessDescriptor d) {
		long next = d != null ? d.id : 0L;
		if (next == e.defaultProviderId)
			return;
		e.defaultProviderId = next;
		editor.setDirty(true);
	}

	private Set<Long> getProviderIds(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptySet();
		var dao = new FlowDao(db);
		return e.isInput
			? dao.getWhereOutput(e.flow.id)
			: dao.getWhereInput(e.flow.id);
	}
}
