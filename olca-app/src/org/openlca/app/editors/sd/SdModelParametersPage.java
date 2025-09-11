package org.openlca.app.editors.sd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

public class SdModelParametersPage extends FormPage {

	private FormToolkit toolkit;
	private TableViewer parameterTable;
	private List<Parameter> parameters = new ArrayList<>();
	private boolean dirty = false;

	public SdModelParametersPage(FormEditor editor) {
		super(editor, "SdModelParametersPage", "Parameters");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		toolkit = mform.getToolkit();
		form.setText("Model Parameters");

		Composite body = form.getBody();
		toolkit.decorateFormHeading(form.getForm());
		toolkit.paintBordersFor(body);
		UI.gridLayout(body, 1);

		createParametersSection(body);
		loadData();
	}

	private void createParametersSection(Composite parent) {
		Section section = toolkit.createSection(parent,
			Section.TITLE_BAR | Section.EXPANDED);
		section.setText("Parameters");
		UI.gridData(section, true, true);

		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);

		parameterTable = Tables.createViewer(comp,
			"Name", "Value", "Unit", "Description");
		parameterTable.setLabelProvider(new ParameterLabelProvider());
		Tables.bindColumnWidths(parameterTable, 0.25, 0.25, 0.15, 0.35);

		// Make table fill the section
		UI.gridData(parameterTable.getControl(), true, true);
	}

	private void loadData() {
		// TODO: Load parameters from model file
		// For now, add some example parameters
		parameters.clear();
		parameters.add(new Parameter("initial_population", "1000", "persons", "Initial population"));
		parameters.add(new Parameter("growth_rate", "0.02", "1/year", "Population growth rate"));
		parameters.add(new Parameter("carrying_capacity", "10000", "persons", "Maximum sustainable population"));

		parameterTable.setInput(parameters);
		dirty = false;
	}

	public void doSave(IProgressMonitor monitor) {
		if (!dirty)
			return;

		// TODO: Save parameters to model file
		// For now, just mark as clean
		dirty = false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	private static class Parameter {
		String name;
		String value;
		String unit;
		String description;

		Parameter(String name, String value, String unit, String description) {
			this.name = name;
			this.value = value;
			this.unit = unit;
			this.description = description;
		}
	}

	private static class ParameterLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter param))
				return null;

			return switch (columnIndex) {
				case 0 -> param.name;
				case 1 -> param.value;
				case 2 -> param.unit;
				case 3 -> param.description;
				default -> null;
			};
		}
	}
}
