package org.openlca.app.editors.processes.exchanges;

import org.openlca.app.db.Cache;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;

class ProviderCombo extends ComboBoxCellModifier<Exchange, RootDescriptor> {

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
	protected RootDescriptor[] getItems(Exchange e) {
		var map = Cache.getMatrixCache().getProviderMap();
		if (map == null || e == null || e.flow == null)
			return new RootDescriptor[0];
		return map.getProvidersOf(e.flow.id)
				.stream()
				.map(TechFlow::provider)
				.sorted((p1, p2) -> Strings.compare(Labels.name(p1), Labels.name(p2)))
				.toArray(RootDescriptor[]::new);
	}

	@Override
	protected RootDescriptor getItem(Exchange e) {
		if (e.defaultProviderId == 0L)
			return null;
		var map = Cache.getMatrixCache().getProviderMap();
		return map != null
				? map.getProvider(e.defaultProviderId)
				: null;
	}

	@Override
	protected String getText(RootDescriptor d) {
		return d != null
				? Labels.name(d)
				: "";
	}

	@Override
	protected void setItem(Exchange e, RootDescriptor d) {
		long next = d != null ? d.id : 0L;
		if (next == e.defaultProviderId)
			return;
		e.defaultProviderId = next;
		e.defaultProviderType = d != null
				? ProviderType.of(d.type)
				: ProviderType.PROCESS;
		editor.setDirty(true);
	}
}
