package org.openlca.app.editors.processes.exchanges;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
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
		FlowType type = e.flow.flowType;
		if (e.isInput)
			return type == FlowType.PRODUCT_FLOW;
		else
			return type == FlowType.WASTE_FLOW;
	}

	@Override
	protected ProcessDescriptor[] getItems(Exchange e) {
		if (e.flow == null)
			return new ProcessDescriptor[0];
		Set<Long> providerIds = getProcessIds(e);
		Collection<ProcessDescriptor> list = cache.getAll(
				ProcessDescriptor.class, providerIds).values();
		ProcessDescriptor[] providers = list.toArray(
				new ProcessDescriptor[list.size()]);
		Arrays.sort(providers, (p1, p2) -> Strings.compare(
				Labels.getDisplayName(p1), Labels.getDisplayName(p2)));
		return providers;
	}

	@Override
	protected ProcessDescriptor getItem(Exchange e) {
		if (e.defaultProviderId == 0)
			return null;
		return cache.get(ProcessDescriptor.class, e.defaultProviderId);
	}

	@Override
	protected String getText(ProcessDescriptor d) {
		if (d == null)
			return M.None;
		return Labels.getDisplayName(d);
	}

	@Override
	protected void setItem(Exchange e, ProcessDescriptor d) {
		if (d == null)
			e.defaultProviderId = 0;
		else
			e.defaultProviderId = d.id;
		editor.setDirty(true);
	}

	private Set<Long> getProcessIds(Exchange e) {
		Set<Long> ids = new HashSet<>();
		if (e == null || e.flow == null)
			return ids;
		FlowDao dao = new FlowDao(db);
		if (e.isInput)
			return dao.getWhereOutput(e.flow.id);
		else
			return dao.getWhereInput(e.flow.id);
	}
}