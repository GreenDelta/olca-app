package org.openlca.app.wizards;

import java.util.Collections;
import java.util.List;

import org.openlca.app.wizards.ProviderDialog.Options;
import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.matrix.LinkingCallback;
import org.openlca.core.matrix.ProcessProduct;

import gnu.trove.map.hash.TLongObjectHashMap;

public class ProviderCallback implements LinkingCallback {

	private boolean autoSelect = false;
	private boolean cancel = false;
	private final TLongObjectHashMap<ProcessProduct> savedSelections = new TLongObjectHashMap<>();

	@Override
	public boolean cancel() {
		return cancel;
	}

	@Override
	public List<ProcessProduct> select(CalcExchange e, List<ProcessProduct> candidates) {
		if (e == null || candidates == null)
			return null;
		if (autoSelect)
			return candidates;
		ProcessProduct saved = savedSelections.get(e.flowId);
		if (saved != null)
			return Collections.singletonList(saved);
		Options opts = ProviderDialog.select(e, candidates);
		if (opts == null || opts.selected == null)
			return candidates;
		autoSelect = opts.autoContinue;
		cancel = opts.cancel;
		if (opts.saveSelected) {
			savedSelections.put(e.flowId, opts.selected);
		}
		return Collections.singletonList(opts.selected);
	}
}
