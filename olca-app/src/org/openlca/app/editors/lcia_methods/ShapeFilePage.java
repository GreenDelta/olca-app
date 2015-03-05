package org.openlca.app.editors.lcia_methods;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.parameters.ModelParameterPage;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.Question;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows imported shape-files and parameters from these shape-files that can be
 * used in a localised LCIA method.
 */
class ShapeFilePage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ImpactMethod method;
	private FormToolkit toolkit;
	private Composite body;
	private ShapeFileSection[] sections;
	private ScrolledForm form;
	private ImpactMethodEditor editor;

	public ShapeFilePage(ImpactMethodEditor editor) {
		super(editor, "ShapeFilePage", "Shape files (beta)");
		this.method = editor.getModel();
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, "Shape file parameters");
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		createFileSection();
		List<String> shapeFiles = ShapeFileUtils.getShapeFiles(method);
		sections = new ShapeFileSection[shapeFiles.size()];
		for (int i = 0; i < shapeFiles.size(); i++)
			sections[i] = new ShapeFileSection(i, shapeFiles.get(i));
		form.reflow(true);
	}

	private void createFileSection() {
		Composite composite = UI.formSection(body, toolkit, "Files");
		createFolderLink(composite);
		UI.formLabel(composite, toolkit, "");
		Button importButton = toolkit.createButton(composite, Messages.Import,
				SWT.NONE);
		importButton.setImage(ImageType.IMPORT_ICON.get());
		Controls.onSelect(importButton, (e) -> {
			File file = FileChooser.forImport("*.shp");
			if (file != null)
				checkRunImport(file);
		});
	}

	private void createFolderLink(Composite composite) {
		UI.formLabel(composite, toolkit, "Location");
		ImageHyperlink link = toolkit.createImageHyperlink(composite, SWT.TOP);
		final File folder = ShapeFileUtils.getFolder(method);
		link.setText(Strings.cut(folder.getAbsolutePath(), 75));
		link.setImage(ImageType.FOLDER_SMALL.get());
		link.setForeground(Colors.getLinkBlue());
		link.setToolTipText(folder.getAbsolutePath());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				openFolder(folder);
			}
		});
	}

	private void openFolder(final File folder) {
		try {
			File f = folder;
			do {
				if (f.exists()) {
					Desktop.getDesktop().open(f);
					break;
				}
				f = f.getParentFile();
			} while (f != null);
		} catch (Exception ex) {
			log.error("failed to open shape-file folder", ex);
		}
	}

	private void checkRunImport(File file) {
		if (!ShapeFileUtils.isValid(file)) {
			org.openlca.app.util.Error.showBox("Invalid file", "The file "
					+ file.getName() + " is not a valid shape file.");
			return;
		}
		if (ShapeFileUtils.alreadyExists(method, file)) {
			org.openlca.app.util.Error
					.showBox(
							"File already exists",
							"A shape "
									+ "file with the given name already exists for this method.");
			return;
		}
		try {
			runImport(file);
		} catch (Exception e) {
			log.error("Failed to import shape file " + file, e);
		}
	}

	private void runImport(File file) throws Exception {
		String shapeFile = ShapeFileUtils.importFile(method, file);
		ShapeFileSection section = new ShapeFileSection(sections.length,
				shapeFile);
		ShapeFileSection[] newSections = new ShapeFileSection[sections.length + 1];
		System.arraycopy(sections, 0, newSections, 0, sections.length);
		newSections[sections.length] = section;
		this.sections = newSections;
		List<ShapeFileParameter> params = ShapeFileUtils.getParameters(method,
				shapeFile);
		section.parameterTable.viewer.setInput(params);
		form.reflow(true);
	}

	private class ShapeFileSection {

		private int index;
		private String shapeFile;
		private Section section;
		private ShapeFileParameterTable parameterTable;

		ShapeFileSection(int index, String shapeFile) {
			this.index = index;
			this.shapeFile = shapeFile;
			render();
		}

		private void render() {
			section = UI.section(body, toolkit, "Parameters of " + shapeFile);
			Composite composite = UI.sectionClient(section, toolkit);
			parameterTable = new ShapeFileParameterTable(shapeFile, composite);
			Action delete = Actions.onRemove(new Runnable() {
				@Override
				public void run() {
					delete();
				}
			});
			ShowMapAction showAction = new ShowMapAction(this);
			AddParamAction addAction = new AddParamAction(this);
			Actions.bind(section, showAction, delete);
			Actions.bind(parameterTable.viewer, showAction, addAction);
		}

		private void delete() {
			boolean del = Question.ask("@Delete " + shapeFile + "?", "Do you "
					+ "really want to delete " + shapeFile + "?");
			if (!del)
				return;
			ShapeFileUtils.deleteFile(method, shapeFile);
			section.dispose();
			ShapeFileSection[] newSections = new ShapeFileSection[sections.length - 1];
			System.arraycopy(sections, 0, newSections, 0, index);
			if ((index + 1) < sections.length)
				System.arraycopy(sections, index + 1, newSections, index,
						newSections.length - index);
			sections = newSections;
			form.reflow(true);
		}
	}

	private class ShapeFileParameterTable {

		private String[] columns = { Messages.Name, Messages.Minimum,
				Messages.Maximum };
		private TableViewer viewer;
		private List<ShapeFileParameter> params;

		ShapeFileParameterTable(String shapeFile, Composite parent) {
			viewer = Tables.createViewer(parent, columns);
			viewer.setLabelProvider(new ShapeFileParameterLabel());
			Tables.bindColumnWidths(viewer, 0.4, 0.3, 0.3);
			try {
				params = ShapeFileUtils.getParameters(method, shapeFile);
				viewer.setInput(params);
			} catch (Exception e) {
				log.error("failed to read parameteres for shape file "
						+ shapeFile, e);
			}
		}

		private class ShapeFileParameterLabel extends LabelProvider implements
				ITableLabelProvider {

			@Override
			public Image getColumnImage(Object o, int i) {
				if (i == 0)
					return ImageType.FORMULA_ICON.get();
				else
					return null;
			}

			@Override
			public String getColumnText(Object o, int i) {
				if (!(o instanceof ShapeFileParameter))
					return null;
				ShapeFileParameter p = (ShapeFileParameter) o;
				switch (i) {
				case 0:
					return p.getName();
				case 1:
					return Double.toString(p.getMin());
				case 2:
					return Double.toString(p.getMax());
				default:
					return null;
				}
			}
		}
	}

	private class ShowMapAction extends Action {

		private ShapeFileSection section;

		public ShowMapAction(ShapeFileSection section) {
			this.section = section;
			setToolTipText("Show in map");
			setText("Show in map");
			setImageDescriptor(ImageType.LCIA_ICON.getDescriptor());
		}

		@Override
		public void run() {
			if (section == null || section.parameterTable == null)
				return;
			ShapeFileParameter param = Viewers
					.getFirstSelected(section.parameterTable.viewer);
			if (param == null)
				ShapeFileUtils.openFileInMap(method, section.shapeFile);
			else
				ShapeFileUtils.openFileInMap(method, section.shapeFile, param);
		}
	}

	private class AddParamAction extends Action {

		private ShapeFileSection section;

		public AddParamAction(ShapeFileSection section) {
			this.section = section;
			setToolTipText("Add to method parameters");
			setText("Add to method parameters");
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
		}

		@Override
		public void run() {
			ShapeFileParameter param = getSelected();
			if (param == null || exists(param))
				return;
			if (otherExists(param)) {
				Error.showBox("Parameter with same name exists",
						"An other parameter with the same name already exists "
								+ "in this LCIA method");
				return;
			}
			addParam(param);
		}

		private ShapeFileParameter getSelected() {
			if (section == null || section.parameterTable == null)
				return null;
			ShapeFileParameter param = Viewers
					.getFirstSelected(section.parameterTable.viewer);
			if (param == null) {
				Error.showBox("No parameter selected", "There is no shapefile "
						+ "parameter selected that could be added as "
						+ "method parameter");
				return null;
			}
			return param;
		}

		private boolean exists(ShapeFileParameter param) {
			for (Parameter realParam : method.getParameters()) {
				if (Strings.nullOrEqual(param.getName(), realParam.getName())
						&& Strings.nullOrEqual("SHAPE_FILE",
								realParam.getSourceType())
						&& Strings.nullOrEqual(section.shapeFile,
								realParam.getExternalSource()))
					return true;
			}
			return false;
		}

		private boolean otherExists(ShapeFileParameter param) {
			for (Parameter realParam : method.getParameters()) {
				if (Strings.nullOrEqual(param.getName(), realParam.getName())
						&& !Strings.nullOrEqual(section.shapeFile,
								realParam.getExternalSource()))
					return true;
			}
			return false;
		}

		private void addParam(ShapeFileParameter param) {
			Parameter realParam = new Parameter();
			realParam.setExternalSource(section.shapeFile);
			realParam.setInputParameter(true);
			realParam.setName(param.getName());
			realParam.setDescription("from shapefile: " + section.shapeFile);
			realParam.setValue((param.getMin() + param.getMax()) / 2);
			realParam.setUncertainty(Uncertainty.uniform(param.getMin(),
					param.getMax()));
			realParam.setScope(ParameterScope.IMPACT_METHOD);
			realParam.setSourceType("SHAPE_FILE");
			method.getParameters().add(realParam);
			editor.setActivePage(ModelParameterPage.ID);
			editor.setDirty(true);
		}
	}

}
