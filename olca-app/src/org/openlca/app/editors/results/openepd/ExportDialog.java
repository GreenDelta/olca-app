package org.openlca.app.editors.results.openepd;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ResultImpact;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

import com.google.gson.GsonBuilder;

public class ExportDialog extends FormDialog {

	private final Ec3Epd epd;
	private final Credentials credentials;
	private String selectedScope = "A1A2A3";

	private final Ec3ImpactModel impactModel;
	private Ec3ImpactModel.Ec3ImpactMethod selectedMethod;
	private List<ImpactItem> impacts;

	private ExportDialog(ResultModel result) {
		super(UI.shell());
		setBlockOnOpen(true);
		setShellStyle(SWT.CLOSE
			| SWT.MODELESS
			| SWT.BORDER
			| SWT.TITLE
			| SWT.RESIZE
			| SWT.MIN);
		credentials = Credentials.init();
		epd = new Ec3Epd();
		epd.name = result.name;
		epd.description = result.description;
		epd.isPrivate = true;
		epd.isDraft = true;

		impactModel = Ec3ImpactModel.read();
		impactModel.methods.sort((m1, m2) -> Strings.compare(m1.name, m2.name));
		if (result.setup != null && result.setup.impactMethod() != null) {
			var d = Descriptor.of(result.setup.impactMethod());
			selectedMethod = impactModel.map(d);
			impacts = ImpactItem.createAll(selectedMethod, result);
		}
	}

	public static int show(ResultModel result) {
		return new ExportDialog(result).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export as OpenEPD to EC3");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 700);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		credentialsSection(body, tk);
		metaSection(body, tk);
		createImpactSection(body, tk);
	}

	private void credentialsSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "EC3 Login");
		var comp = UI.sectionClient(section, tk, 2);

		// url
		var filled = 0;
		var urlText = UI.formText(comp, tk, "URL");
		if (Strings.notEmpty(credentials.url)) {
			urlText.setText(credentials.url);
			filled++;
		}
		urlText.addModifyListener(
			$ -> credentials.url = urlText.getText());

		// user
		var userText = UI.formText(comp, tk, "User");
		if (Strings.notEmpty(credentials.user)) {
			userText.setText(credentials.user);
			filled++;
		}
		userText.addModifyListener(
			$ -> credentials.user = userText.getText());

		// password
		var pwText = UI.formText(comp, tk, "Password", SWT.PASSWORD);
		if (Strings.notEmpty(credentials.password)) {
			pwText.setText(credentials.password);
			filled++;
		}
		pwText.addModifyListener(
			$ -> credentials.password = pwText.getText());

		section.setExpanded(filled < 3);
	}

	private void metaSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.GeneralInformation);
		var comp = UI.sectionClient(section, tk, 2);
		text(UI.formText(comp, tk, "Name"),
			epd.name, s -> epd.name = s);
		text(UI.formMultiText(comp, tk, "Description"),
			epd.description, s -> epd.description = s);

		var combo = UI.formCombo(comp, tk, "Scope");
		var items = new String[]{
			"A1A2A3", "A1", "A2", "A3", "A4", "A5",
			"B1", "B2", "B3", "B4", "B5", "B6", "B7",
			"C1", "C2", "C3", "C4"};
		combo.setItems(items);
		combo.select(0);
		Controls.onSelect(combo, $ -> {
			var idx = combo.getSelectionIndex();
			selectedScope = combo.getItem(idx);
		});
	}

	private void text(Text text, String initial, Consumer<String> onChange) {
		if (initial != null) {
			text.setText(initial);
		}
		text.addModifyListener($ -> onChange.accept(text.getText()));
	}

	private void createImpactSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.ImpactAssessmentResults);
		var comp = UI.sectionClient(section, tk, 1);

		var comboComp = tk.createComposite(comp);
		UI.gridLayout(comboComp, 2, 10, 0);
		var combo = UI.formCombo(comboComp, tk,
			"EC3 " + M.ImpactAssessmentMethod);
		UI.gridData(combo, false, false).widthHint = 250;

		var methods = impactModel.methods;
		var methodItems = new String[methods.size()];
		var selectedIdx = -1;
		for (int i = 0; i < methods.size(); i++) {
			var method = methods.get(i);
			methodItems[i] = method.name;
			if (Objects.equals(method, selectedMethod)) {
				selectedIdx = i;
			}
		}

		combo.setItems(methodItems);
		if (selectedIdx >= 0) {
			combo.select(selectedIdx);
		}
		Controls.onSelect(combo, $ -> {
			var idx = combo.getSelectionIndex();
			if (idx > 0 && idx < methods.size()) {
				selectedMethod = methods.get(idx);
			}
		});

		TableViewer table = Tables.createViewer(comp,
			"EC3 Indicator",
			"EC3 Unit",
			"Result amount",
			"Result indicator",
			"Result unit");
		table.setLabelProvider(new TableLabel());
		Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);
		if (impacts != null) {
			table.setInput(impacts);
		}
	}

	@Override
	protected void okPressed() {

		/*
		 var client = credentials.login().orElse(null);
		 if (client == null) {
		 MsgBox.error(
		 "Failed to login to EC3",
		 "Could not login to EC3 with the provided credentials.");
		 return;
		 }
		 */

		var file = FileChooser.forSavingFile(
			"Save in OpenEPD format",
			URLEncoder.encode(Strings.orEmpty(epd.name), StandardCharsets.UTF_8)
				+ ".json");
		if (file == null)
			return;
		try {

			// add impacts
			if (impacts != null && selectedMethod != null) {
				var impactSet = new Ec3ImpactSet();
				for (var i : impacts) {
					var indicator = i.indicator;
					var impact = i.impact;
					var amount = Ec3Measurement.of(impact.amount, indicator.unit);
					var scopeSet = new Ec3ScopeSet();
					scopeSet.put(selectedScope, amount);
					impactSet.put(indicator.name, scopeSet);
				}
				if (!impactSet.isEmpty()) {
					epd.putImpactSet(selectedMethod.name, impactSet);
				}
			}

			var json = new GsonBuilder().setPrettyPrinting()
				.create()
				.toJson(epd.toJson());
			Files.writeString(file.toPath(), json);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
		}
	}

	private static class ImpactItem {

		final Ec3ImpactModel.Ec3ImpactIndicator indicator;
		final ResultImpact impact;

		ImpactItem(
			Ec3ImpactModel.Ec3ImpactIndicator indicator,
			ResultImpact impact) {
			this.indicator = indicator;
			this.impact = impact;
		}

		static List<ImpactItem> createAll(
			Ec3ImpactModel.Ec3ImpactMethod selectedMethod, ResultModel result) {
			if (selectedMethod == null
				|| result.setup == null
				|| result.setup.impactMethod() == null)
				return Collections.emptyList();

			var map = selectedMethod.map(result);
			var items = new ArrayList<ImpactItem>();
			for (var indicator : selectedMethod.indicators) {
				var impact = map.get(indicator.name);
				if (impact == null) {
					impact = new ResultImpact();
				}
				items.add(new ImpactItem(indicator, impact));
			}

			items.sort((item1, item2) -> Strings.compare(
				item1.indicator.name, item2.indicator.name));
			return items;
		}
	}

	private static class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactItem))
				return null;
			var item = (ImpactItem) obj;
			var indicator = item.indicator;
			var impact = item.impact;
			if (indicator == null || impact == null)
				return null;

			return switch (col) {
				case 0 -> indicator.name;
				case 1 -> indicator.unit;
				case 2 -> Numbers.format(impact.amount);
				case 3 -> impact.indicator != null
					? impact.indicator.name
					: "---";
				case 4 -> impact.indicator != null
					? impact.indicator.referenceUnit
					: "---";
				default -> null;
			};
		}
	}
}
