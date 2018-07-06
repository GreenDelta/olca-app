package org.openlca.app.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class InfoSection {

	public static void create(Composite body, FormToolkit toolkit, CalculationSetup setup, String type) {
		if (setup == null || setup.productSystem == null)
			return;
		Composite comp = UI.formSection(body, toolkit, M.GeneralInformation);
		link(comp, toolkit, M.ProductSystem, setup.productSystem);
		text(comp, toolkit, M.AllocationMethod, Labels.getEnumText(setup.allocationMethod));
		text(comp, toolkit, M.TargetAmount, targetAmountText(setup));
		if (setup.impactMethod != null) {
			link(comp, toolkit, M.ImpactAssessmentMethod, setup.impactMethod);
		}
		if (setup.nwSet != null) {
			text(comp, toolkit, M.NormalizationAndWeightingSet, setup.nwSet.getName());
		}
		exportButton(comp, toolkit, type);
	}

	private static String targetAmountText(CalculationSetup setup) {
		String refFlowName = setup.productSystem.referenceExchange.flow.getName();
		return setup.getAmount() + " " + setup.getUnit().getName() + " " + refFlowName;
	}

	static void text(Composite comp, FormToolkit toolkit, String label, String val) {
		Text text = UI.formText(comp, toolkit, label);
		text.setText(val);
		text.setEditable(false);
	}

	static void link(Composite parent, FormToolkit toolkit, String label, Object entity) {
		new Label(parent, SWT.NONE).setText(label);
		ImageHyperlink link = new ImageHyperlink(parent, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		if (entity instanceof CategorizedDescriptor)
			decorateLink(link, (CategorizedDescriptor) entity);
		else if (entity instanceof CategorizedEntity)
			decorateLink(link, (CategorizedEntity) entity);
	}

	private static void decorateLink(ImageHyperlink link, CategorizedEntity entity) {
		link.setText(Labels.getDisplayName(entity));
		link.setImage(Images.get(entity));
		Controls.onClick(link, (e) -> App.openEditor(entity));
	}

	private static void decorateLink(ImageHyperlink link, CategorizedDescriptor entity) {
		link.setText(Labels.getDisplayName(entity));
		link.setImage(Images.get(entity));
		Controls.onClick(link, (e) -> App.openEditor(entity));
	}

	private static void exportButton(Composite composite, FormToolkit toolkit, String type) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, M.ExportToExcel, SWT.NONE);
		button.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(button, (e) -> new ExcelExportAction().run());
	}
}
