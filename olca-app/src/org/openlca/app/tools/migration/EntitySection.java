package org.openlca.app.tools.migration;

import java.util.ArrayList;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Strings;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.olca.migration.MigrationPlan;

final class EntitySection {

	private final MigrationPlan plan;

	private EntitySection(MigrationPlan plan) {
		this.plan = plan;
	}

	static void create(MigrationPlan plan, Composite parent, FormToolkit tk) {
		new EntitySection(plan).render(parent, tk);
	}

	private void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Selected entities for migration");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, "Entity", "Status");
		table.setLabelProvider(new EntityLabel());

		var items = collectEntities();
		table.setInput(items);
		Tables.bindColumnWidths2(table, 0.6, 0.4);
		var gd = UI.gridData(table.getTable(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;
	}

	private ArrayList<EntityItem> collectEntities() {
		var items = new ArrayList<EntityItem>();
		for (var s : plan.systems()) {
			items.add(new EntityItem(Descriptor.of(s), "Will be migrated"));
		}
		for (var e : plan.entityCopies()) {
			items.add(new EntityItem(e, "Will be migrated"));
		}
		for (var e : plan.entityMatches()) {
			items.add(new EntityItem(e, "Exists already"));
		}
		items.sort((a, b) -> {
			var typeCmp = ModelTypeOrder.compare(
				a.entity().type, b.entity().type);
			return typeCmp != 0
				? typeCmp
				: Strings.compareNatural(
					Labels.name(a.entity()), Labels.name(b.entity()));
		});
		return items;
	}

	private record EntityItem(RootDescriptor entity, String status) {
	}

	private static final class EntityLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof EntityItem(RootDescriptor entity, String ignored))
				|| col != 0)
				return null;
			return Images.get(entity);
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof EntityItem(RootDescriptor entity, String status)))
				return null;
			return switch (col) {
				case 0 -> Labels.name(entity);
				case 1 -> status;
				default -> null;
			};
		}
	}
}
