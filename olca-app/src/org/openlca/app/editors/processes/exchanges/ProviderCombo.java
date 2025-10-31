package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Exchanges;
import org.openlca.util.Strings;

class ProviderCombo extends ComboBoxViewerCellEditor {

	private final ProcessEditor editor;
	private Exchange exchange;
	private final LabelProvider labels;

	ProviderCombo(TableViewer table, ProcessEditor editor) {
		super(table.getTable());
		this.editor = editor;
		this.labels = new LabelProvider();
		setLabelProvider(labels);
		setContentProvider(ArrayContentProvider.getInstance());
		setValidator(obj -> obj instanceof Exchange e && Exchanges.isLinkable(e)
				? null
				: "not linkable");
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

		var selected = Item.nil();
		var items = new ArrayList<Item>();
		items.add(selected);
		var types = EnumSet.noneOf(ModelType.class);
		for (var p : providers) {
			var item = Item.of(p.provider());
			items.add(item);
			if (e.defaultProviderId == p.providerId()) {
				selected = item;
			}
			var type = item.modelType();
			if (type != null) {
				types.add(type);
			}
		}

		labels.mixedTypes = types.size() > 1;
		Collections.sort(items);
		setInput(items);
		getViewer().setSelection(new StructuredSelection(selected));
	}

	@Override
	protected Object doGetValue() {
		if (exchange == null)
			return null;
		var selection = Viewers.getFirstSelected(getViewer());
		if (!(selection instanceof Item item))
			return null;
		if (exchange.defaultProviderId == item.providerId())
			return null;
		exchange.defaultProviderId = item.providerId();
		exchange.defaultProviderType = item.providerType();
		editor.setDirty();
		return exchange;
	}

	private static class LabelProvider extends BaseLabelProvider
			implements ILabelProvider {

		private boolean mixedTypes = false;

		@Override
		public Image getImage(Object obj) {
			return obj instanceof Item(RootDescriptor d) && d != null
					? getImage(d)
					: null;
		}

		@Override
		public String getText(Object obj) {
			if (!(obj instanceof Item item) || item.provider == null)
				return M.NoneHyphen;
			var d = item.provider;
			var name = Labels.name(d);
			if (!mixedTypes)
				return name;
			return switch (item.modelType()) {
				case PROCESS -> name;
				case RESULT -> "[R] " + name;
				case PRODUCT_SYSTEM -> "[S] " + name;
				case null, default -> "[?] " + name;
			};
		}
	}

	private record Item(RootDescriptor provider) implements Comparable<Item> {

		static Item of(RootDescriptor provider) {
			return new Item(provider);
		}

		static Item nil() {
			return new Item(null);
		}

		boolean isNil() {
			return provider == null;
		}

		long providerId() {
			return provider != null ? provider.id : 0L;
		}

		byte providerType() {
			return provider != null
					? ProviderType.of(provider.type)
					: ProviderType.PROCESS;
		}

		ModelType modelType() {
			return provider != null && provider.type != null
				? provider.type
				: null;
		}

		@Override
		public int compareTo(Item other) {
			if (other.isNil())
				return this.isNil() ? 0 : 1;
			if (this.isNil())
				return -1;

			byte ti = providerType();
			byte to = other.providerType();
			if (ti != to)
				return ti - to;

			return Strings.compareIgnoreCase(
				Labels.name(this.provider), Labels.name(other.provider));
		}
	}
}
