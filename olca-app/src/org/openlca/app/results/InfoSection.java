package org.openlca.app.results;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.RootDescriptor;

public class InfoSection {

	static void create(Composite body, FormToolkit tk, CalculationSetup setup) {
		if (setup == null)
			return;
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		setupRows(comp, tk, setup, true);
		buttons(comp, tk);
	}

	/// Renders the rows with the general information of the given calculation
	/// setup in the given composite, which needs to have a grid layout with
	/// two columns. This is used for the result and simulation editors, so that
	/// they show this information in the same way.
	///
	/// @param withNwSet whether the normalization and weighting set should be
	///                  shown when it is defined in the setup
	public static void setupRows(
		Composite comp, FormToolkit tk, CalculationSetup setup, boolean withNwSet
	) {
		if (setup == null)
			return;
		if (setup.hasProductSystem()) {
			link(comp, tk, M.ProductSystem, setup.productSystem());
		} else {
			link(comp, tk, M.ReferenceProcess, setup.process());
		}
		if (setup.impactMethod() != null) {
			link(comp, tk, M.ImpactAssessmentMethod, setup.impactMethod());
			if (withNwSet && setup.nwSet() != null) {
				text(comp, tk, M.NormalizationAndWeightingSet, setup.nwSet().name);
			}
		}
		text(comp, tk, M.TargetAmount, targetAmountText(setup));
		text(comp, tk, M.AllocationMethod, Labels.getEnumText(setup.allocation()));
		var paramSet = parameterSetNameOf(setup);
		if (paramSet != null) {
			text(comp, tk, M.ParameterSet, paramSet);
		}
	}

	/// Returns the name of the parameter set of the given setup, but only if
	/// the product system has more than one parameter set. Otherwise, there
	/// was no choice: the calculation wizard then automatically uses the only
	/// (baseline) parameter set and showing its name would be just noise.
	/// Returns `null` if no parameter set name should be shown.
	static String parameterSetNameOf(CalculationSetup setup) {
		if (setup == null
			|| !setup.hasProductSystem()
			|| Strings.isBlank(setup.parameterSetName()))
			return null;
		var system = setup.productSystem();
		return system != null && system.parameterSets.size() > 1
			? setup.parameterSetName()
			: null;
	}

	private static String targetAmountText(CalculationSetup setup) {
		return amountText(setup.amount())
			+ " " + Labels.name(setup.unit())
			+ " " + Labels.name(setup.flow());
	}

	/// Formats the absolute value of the target amount without unnecessary
	/// decimal places, e.g. `1`, `100`, or `0.25`. Very small amounts are
	/// shown in scientific notation.
	private static String amountText(double amount) {
		var abs = Math.abs(amount);
		if (abs != 0 && abs < 1e-4)
			return Double.toString(abs);
		var format = new DecimalFormat(
			"0.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return format.format(abs);
	}

	static void text(Composite comp, FormToolkit tk, String label, String val) {
		Text text = UI.labeledText(comp, tk, label);
		if (val != null) {
			text.setText(val);
		}
		text.setEditable(false);
	}

	static void link(Composite comp, FormToolkit tk, String label, Object entity) {
		UI.label(comp, tk, label);
		var link = UI.imageHyperlink(comp, tk, SWT.TOP);
		if (entity instanceof RootDescriptor d) {
			link.setText(Labels.name(d));
			link.setImage(Images.get(d));
			Controls.onClick(link, _ -> App.open(d));
		} else if (entity instanceof RootEntity ce) {
			link.setText(Labels.name(ce));
			link.setImage(Images.get(ce));
			Controls.onClick(link, _ -> App.open(ce));
		}
	}

	private static void buttons(Composite comp, FormToolkit tk) {
		UI.label(comp, tk, "");
		var inner = UI.composite(comp, tk);
		UI.gridLayout(inner, 2, 5, 0);
		var excelBtn = UI.button(inner, tk, M.ExportToExcel);
		excelBtn.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(excelBtn, _ -> ExcelExportWizard.open());
		var resultBtn = UI.button(inner, tk, M.SaveResultAsDots, SWT.NONE);
		resultBtn.setImage(Icon.SAVE_AS.get());
		Controls.onSelect(resultBtn, _ -> {
			ResultEditor editor = Editors.getActive();
			if (editor == null)
				return;
			SaveResultDialog.open(editor);
		});

	}
}
