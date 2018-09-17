package org.openlca.app.wizards;

import org.openlca.app.wizards.ProviderDialog.Options;
import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.matrix.LinkingCallback;

import gnu.trove.map.hash.TLongLongHashMap;

public class ProviderCallback implements LinkingCallback {

	private boolean autoSelect = false;
	private boolean cancel = false;
	private final TLongLongHashMap savedSelections = new TLongLongHashMap();

	@Override
	public boolean cancel() {
		return cancel;
	}

	@Override
	public long[] select(CalcExchange e, long[] candidates) {
		if (e == null || candidates == null)
			return null;
		if (autoSelect)
			return candidates;
		long saved = savedSelections.get(e.flowId);
		if (saved > 0L)
			return new long[] { saved };
		Options opts = ProviderDialog.select(e, candidates);
		if (opts == null)
			return candidates;
		autoSelect = opts.autoContinue;
		cancel = opts.cancel;
		if (opts.saveSelected) {
			savedSelections.put(e.flowId, opts.selected);
		}
		return new long[] { opts.selected };
	}
}
