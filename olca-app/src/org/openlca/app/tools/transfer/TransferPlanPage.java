package org.openlca.app.tools.transfer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.olca.systransfer.ProviderInfo;
import org.openlca.io.olca.systransfer.TransferConfig;
import org.openlca.io.olca.systransfer.TransferPlan;

final class TransferPlanPage extends FormPage {

	private final TransferPlanEditor editor;
	private final TransferPlan plan;
	private final TransferConfig config;

	TransferPlanPage(TransferPlanEditor editor) {
		super(editor, "TransferPlanEditor.Page", "Transfer plan");
		this.editor = editor;
		this.plan = editor.plan();
		this.config = editor.config();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "Transfer plan", Icon.TARGET.get());
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
		ModelLink.of(ProductSystem.class)
			.setEditable(false)
			.setModel(config.system())
			.renderOn(comp, tk, M.ProductSystem);

		UI.filler(comp, tk);
		var button = UI.button(comp, tk, "Transfer");
		button.setImage(Icon.RUN.get());
		Controls.onSelect(button, $ -> editor.runTransfer());
	}

	private void createCopiesSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Providers copied to target database");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, "Provider", "Flow", "Location");
		table.setLabelProvider(new ProviderInfoLabel());
		table.setInput(plan.copies());
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
