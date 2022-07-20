package org.openlca.app.editors.systems;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.Demand;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.index.ImpactIndex;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.matrix.io.MatrixExport;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.xls.MatrixExcelExport;
import org.openlca.util.Strings;

public class MatrixExportDialog extends FormDialog {

	private final IDatabase db;
	private final ProductSystem system;
	private final Config config = new Config();

	/**
	 * Opens a dialog for exporting the complete database in to a matrix format.
	 */
	public static void open(IDatabase db) {
		open(db, null);
	}

	/**
	 * Opens a dialog for exporting the given product system into a matrix format.
	 */
	public static void open(IDatabase db, ProductSystem system) {
		if (db == null)
			return;
		new MatrixExportDialog(db, system).open();
	}

	private MatrixExportDialog(IDatabase db, ProductSystem system) {
		super(UI.shell());
		this.db = Objects.requireNonNull(db);
		this.system = system;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export matrices");
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		var control = super.createButtonBar(parent);
		// we disable the OK button until a folder is selected
		var ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) {
			ok.setEnabled(false);
		}
		return control;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 2);

		fileSelection(body, tk);
		formatSelection(body, tk);
		parametersCombo(body, tk);
		allocationCombo(body, tk);
		methodCombo(body, tk);

		// check boxes
		BiConsumer<String, Consumer<Boolean>> check = (label, fn) -> {
			UI.filler(body, tk);
			var button = tk.createButton(body, label, SWT.CHECK);
			Controls.onSelect(button, _e -> fn.accept(button.getSelection()));
		};
		check.accept("Regionalized", b -> config.regionalized = b);
		check.accept("With costs", b -> config.withCosts = b);
		check.accept("With uncertainty distributions",
			b -> config.withUncertainties = b);
	}

	private void fileSelection(Composite body, FormToolkit tk) {
		UI.formLabel(body, tk, M.Folder);
		var inner = tk.createComposite(body);
		UI.gridData(inner, true, false);
		UI.gridLayout(inner, 2, 5, 0);
		var fileText = tk.createText(inner, "");
		UI.gridData(fileText, true, false);
		fileText.setEditable(false);
		var browseBtn = tk.createButton(inner, M.Browse, SWT.NONE);
		Controls.onSelect(browseBtn, _e -> {
			var folder = FileChooser.selectFolder();
			if (folder == null)
				return;
			config.folder = folder;
			fileText.setText(folder.getAbsolutePath());
			var ok = getButton(IDialogConstants.OK_ID);
			if (ok != null) {
				ok.setEnabled(true);
			}
		});
	}

	private void formatSelection(Composite body, FormToolkit tk) {
		UI.formLabel(body, tk, "Format");
		var inner = tk.createComposite(body);
		UI.gridData(inner, true, false);
		var formats = Format.values();
		UI.gridLayout(inner, formats.length, 10, 0);
		for (var format : formats) {
			var radio = tk.createButton(
				inner, format.toString(), SWT.RADIO);
			if (format == config.format) {
				radio.setSelection(true);
			}
			Controls.onSelect(radio, _e -> {
				if (radio.getSelection()) {
					config.format = format;
				}
			});
		}
	}

	private void parametersCombo(Composite comp, FormToolkit tk) {
		if (system == null)
			return;
		var paramSets = new ArrayList<>(system.parameterSets);
		if (paramSets.size() < 2)
			return;

		paramSets.sort((s1, s2) -> {
			if (s1.isBaseline)
				return -1;
			if (s2.isBaseline)
				return 1;
			return Strings.compare(s1.name, s2.name);
		});

		UI.formLabel(comp, tk, "Parameter set");
		var combo = new TableCombo(comp,
			SWT.READ_ONLY | SWT.BORDER);
		tk.adapt(combo);
		UI.gridData(combo, true, false);
		for (var paramSet : paramSets) {
			var item = new TableItem(
				combo.getTable(), SWT.NONE);
			item.setText(paramSet.name);
		}

		combo.select(0);
		config.parameters = paramSets.get(0);
		Controls.onSelect(combo, e -> {
			int i = combo.getSelectionIndex();
			config.parameters = paramSets.get(i);
		});
	}

	private void allocationCombo(Composite comp, FormToolkit tk) {
		UI.formLabel(comp, tk, M.AllocationMethod);
		var combo = new AllocationCombo(
			comp, AllocationMethod.values());
		combo.setNullable(false);
		combo.select(AllocationMethod.USE_DEFAULT);
		combo.addSelectionChangedListener(
			method -> config.allocation = method);
	}

	private void methodCombo(Composite comp, FormToolkit tk) {
		UI.formLabel(comp, tk, M.ImpactAssessmentMethod);
		var combo = new ImpactMethodViewer(comp);
		combo.setNullable(true);
		combo.setInput(db);
		combo.addSelectionChangedListener(_e -> {
			var d = combo.getSelected();
			if (d != null) {
				config.impactMethod = db.get(ImpactMethod.class, d.id);
			}
		});
	}

	@Override
	protected void okPressed() {
		if (config.folder == null)
			return;

		// check if there are already files in that folder and
		// warn the user when this is the case
		var content = config.folder.listFiles();
		var hasContent = content != null && content.length > 0;
		if (hasContent) {
			var b = Question.ask(
				"Export folder not empty",
				"The export folder is not empty. Existing files " +
				"may are overwritten during the export. Do you " +
				"want to continue?");
			if (!b)
				return;
		}

		super.okPressed();
		App.runWithProgress("Export matrices", () -> {
			try {
				config.exec();
			} catch (Exception e) {
				ErrorReporter.on(
					"Failed to export product system matrices", e);
			}
		});
	}

	private class Config {

		File folder;
		Format format = Format.PYTHON;
		AllocationMethod allocation;
		ImpactMethod impactMethod;
		ParameterRedefSet parameters;
		boolean regionalized;
		boolean withCosts;
		boolean withUncertainties;

		void exec() {
			var techIndex = system == null
				? TechIndex.of(db)
				: TechIndex.of(db, system);
			var config = MatrixData.of(db, techIndex)
				.withAllocation(allocation)
				.withCosts(withCosts)
				.withRegionalization(regionalized)
				.withUncertainties(withUncertainties);
			
			if (system != null) {
				config.withDemand(Demand.of(system));
			}

			if (impactMethod != null) {
				config.withImpacts(ImpactIndex.of(impactMethod));
			}

			// set the parameter redefinitions
			if (parameters != null) {
				config.withParameterRedefs(parameters.parameters);
			} else if (system != null) {
				// if there is exactly one parameter set in
				// the product system we do not show this in
				// the UI but we add it by default here
				if (system.parameterSets.size() == 1) {
					var ps = system.parameterSets.get(0);
					config.withParameterRedefs(ps.parameters);
				}
			}
			var data = config.build();

			switch (format) {
				case CSV -> MatrixExport.toCsv(db, folder, data).writeAll();
				case PYTHON -> {
					MatrixExport.toCsv(db, folder, data).writeIndices();
					MatrixExport.toNpy(db, folder, data).writeMatrices();
					copyResource("MatrixExport_main.py", "main.py");
					copyResource("MatrixExport_lib.py", "lib.py");
				}
				case EXCEL -> new MatrixExcelExport(db, folder, data).writeAll();
			}
			copyResource("MatrixExport_README.md", "README.md");
		}

		private void copyResource(String source, String target) {
			var in = getClass().getResourceAsStream(source);
			if (in == null)
				return;
			try (in) {
				var out = new File(folder, target).toPath();
				Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				ErrorReporter.on("failed to write " + target, e);
			}
		}
	}

	private enum Format {
		CSV, EXCEL, PYTHON;

		@Override
		public String toString() {
			return switch (this) {
				case CSV -> "CSV";
				case EXCEL -> "Excel";
				case PYTHON -> "Python (NumPy, SciPy)";
			};
		}
	}
}
