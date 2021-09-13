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
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class InfoSection {

	static void create(Composite body, FormToolkit tk, CalculationSetup setup) {
		if (setup == null)
			return;
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		if (setup.hasProductSystem()) {
			link(comp, M.ProductSystem, setup.productSystem());
		} else {
			link(comp, M.ReferenceProcess, setup.process());
		}

		text(comp, tk, M.AllocationMethod, Labels.getEnumText(setup.allocation()));
		text(comp, tk, M.TargetAmount, targetAmountText(setup));
		if (setup.impactMethod() != null) {
			link(comp, M.ImpactAssessmentMethod, setup.impactMethod());
		}
		if (setup.nwSet() != null) {
			text(comp, tk, M.NormalizationAndWeightingSet, setup.nwSet().name);
		}
		buttons(comp, tk);
	}

	private static String targetAmountText(CalculationSetup setup) {
		return Math.abs(setup.amount())
				+ " " + Labels.name(setup.unit())
				+ " " + Labels.name(setup.flow());
	}

	static void text(Composite comp, FormToolkit tk, String label, String val) {
		Text text = UI.formText(comp, tk, label);
		if (val != null) {
			text.setText(val);
		}
		text.setEditable(false);
	}

	static void link(Composite comp, String label, Object entity) {
		new Label(comp, SWT.NONE).setText(label);
		var link = new ImageHyperlink(comp, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		if (entity instanceof CategorizedDescriptor) {
			var d = (CategorizedDescriptor) entity;
			link.setText(Labels.name(d));
			link.setImage(Images.get(d));
			Controls.onClick(link, e -> App.open(d));
		} else if (entity instanceof CategorizedEntity) {
			CategorizedEntity ce = (CategorizedEntity) entity;
			link.setText(Labels.name(ce));
			link.setImage(Images.get(ce));
			Controls.onClick(link, e -> App.open(ce));
		}
	}

	private static void buttons(Composite comp, FormToolkit tk) {
		tk.createLabel(comp, "");
		Composite inner = tk.createComposite(comp);
		UI.gridLayout(inner, 2, 5, 0);
		Button excel = tk.createButton(inner,
				M.ExportToExcel, SWT.NONE);
		excel.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(excel,
				e -> new ExcelExportAction().run());
		Button lci = tk.createButton(inner,
				M.SaveAsLCIResult, SWT.NONE);
		lci.setImage(Images.get(ProcessType.LCI_RESULT));
		Controls.onSelect(lci, e -> {
			ResultEditor<?> editor = Editors.getActive();
			if (editor == null)
				return;
			SaveProcessDialog.open(editor);
		});

	}
}
