package org.openlca.app.tools.migration;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.olca.migration.MigrationConfig;
import org.openlca.io.olca.migration.MigrationPlan;
import org.openlca.io.olca.migration.ProviderInfo;

final class MigrationPlanPage extends FormPage {

	private final MigrationPlanEditor editor;
	private final MigrationPlan plan;
	private final MigrationConfig config;

	MigrationPlanPage(MigrationPlanEditor editor) {
		super(editor, "MigrationPlanEditor.Page", "Migration plan");
		this.editor = editor;
		this.plan = editor.plan();
		this.config = editor.config();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Migration plan");
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		createInfoSection(body, tk);
		createCopiesSection(body, tk);
		MatchesSection.create(plan, body, tk);
	}

	private void createInfoSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, "Setup", 2);

		UI.label(comp, tk, "Source database");
		UI.label(comp, tk, config.source().getName());
		UI.label(comp, tk, "Target database");
		UI.label(comp, tk, config.target().getName());

		UI.filler(comp, tk);
		var button = UI.button(comp, tk, "Transfer");
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, $ -> {
			var b = Question.ask("Execute migration?",
				"Do you want to execute the migration now?");
			if (!b) return;
			button.setText("Executed");
			button.setEnabled(false);
			button.getParent().layout();
			editor.runTransfer();
		});
	}

	private void createCopiesSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Providers copied to target database");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, "Provider", "Flow", "Location");
		table.setLabelProvider(new ProviderInfoLabel());
		table.setInput(plan.providerCopies());
		Tables.bindColumnWidths2(table, 0.5, 0.3, 0.2);
		var gd = UI.gridData(table.getTable(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;
	}

	private static final class ProviderInfoLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ProviderInfo(
				FlowDescriptor flow,
				RootDescriptor provider,
				LocationDescriptor location
			)))
				return null;
			return switch (col) {
				case 0 -> Images.get(provider);
				case 1 -> Images.get(flow);
				case 2 -> Images.get(location);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ProviderInfo(
				FlowDescriptor flow,
				RootDescriptor provider,
				LocationDescriptor location
			)))
				return null;
			return switch (col) {
				case 0 -> Labels.name(provider);
				case 1 -> Labels.name(flow);
				case 2 -> Labels.name(location);
				default -> null;
			};
		}
	}
}
