package org.openlca.app.db.tables;

import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.SearchPage;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.Descriptor;

class Actions {

	static void forRootEntities(TableViewer table) {

		var copy = TableClipboard.onCopySelected(table);

		var open = org.openlca.app.util.Actions.onOpen(() -> {
			var obj = Viewers.getFirstSelected(table);
			if (obj instanceof CategorizedEntity e) {
				App.open(e);
			}
		});

		Tables.onDoubleClick(table, $ -> open.run());
		var usage = org.openlca.app.util.Actions.create(
			M.Usage, Icon.LINK.descriptor(), () -> {
				var obj = Viewers.getFirstSelected(table);
				if (obj instanceof CategorizedEntity e) {
					SearchPage.forUsage(Descriptor.of(e));
				}
			});

		org.openlca.app.util.Actions.bind(table, open, usage, copy);
	}

}
