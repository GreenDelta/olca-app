package org.openlca.app.db.tables;

import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.SearchPage;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.function.Function;
import java.util.function.Supplier;

class Actions {

	static void bind(TableViewer table) {
		bind(table, obj -> {
			if (obj instanceof RootDescriptor d)
				return d;
			if (obj instanceof RootEntity e)
				return Descriptor.of(e);
			return null;
		});
	}

	static <T extends RootDescriptor> void bind(
		TableViewer table, Function<Object, T> itemFn) {

		Supplier<T> select = () -> {
			var obj = Viewers.getFirstSelected(table);
			if (obj == null)
				return null;
			return itemFn.apply(obj);
		};

		var copy = TableClipboard.onCopySelected(table);

		var open = org.openlca.app.util.Actions.onOpen(() -> {
			var e = select.get();
			if (e != null) {
				App.open(e);
			}
		});
		Tables.onDoubleClick(table, $ -> open.run());

		var usage = org.openlca.app.util.Actions.create(
			M.Usage, Icon.LINK.descriptor(), () -> {
				var e = select.get();
				if (e != null) {
					SearchPage.forUsage(e);
				}
			});

		org.openlca.app.util.Actions.bind(table, open, usage, copy);
	}

}
