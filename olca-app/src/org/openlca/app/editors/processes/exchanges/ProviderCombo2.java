package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.AppContext;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Exchanges;
import org.openlca.util.Strings;

public class ProviderCombo2 extends ComboBoxViewerCellEditor {

	private Exchange exchange;

	public ProviderCombo2(TableViewer table, ProcessEditor editor) {
		super(table.getTable());
		setLabelProvider(new LabelProvider());
		setContentProvider(ArrayContentProvider.getInstance());
	}

	@Override
	protected void doSetValue(Object value) {
		if (!(value instanceof Exchange e) || !Exchanges.isLinkable(e)) {
			exchange = null;
			deactivate();
			return;
		}
		exchange = e;
		var providers = AppContext.getProviderMap().getProvidersOf(e.flow.id);
		if (providers.isEmpty()) {
			setInput(null);
			return;
		}

		RootDescriptor selected = null;
		var candidates = new ArrayList<RootDescriptor>();
		for (var p : providers) {
			candidates.add(p.provider());
			if (e.defaultProviderId == p.providerId()) {
				selected = p.provider();
			}
		}
		candidates.sort(
				(a, b) -> Strings.compare(Labels.name(a), Labels.name(b)));

		setInput(candidates);
		if (selected != null) {
			getViewer().setSelection(new StructuredSelection(selected));
		}
	}

	@Override
	protected Object doGetValue() {


		return super.doGetValue();
	}

	private static class LabelProvider extends BaseLabelProvider
			implements ILabelProvider {

		@Override
		public Image getImage(Object obj) {
			return obj instanceof RootDescriptor d
					? Images.get(d)
					: null;
		}

		@Override
		public String getText(Object obj) {
			return obj instanceof RootDescriptor d
					? Labels.name(d)
					: "";
		}
	}
}

