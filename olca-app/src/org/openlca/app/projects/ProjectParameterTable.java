package org.openlca.app.projects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Project;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProjectParameterTable {

	private final String PARAMETER = Messages.Parameter;
	private final String PROCESS = Messages.Process;

	private Logger log = LoggerFactory.getLogger(getClass());
	private ProjectEditor editor;
	private EntityCache cache = Database.getCache();
	private Project project;
	private String[] variantNames;
	private List<ParameterRedef> redefs = new ArrayList<>();
	private ModifySupport<ParameterRedef> modifySupport;
	private TableViewer viewer;

	public ProjectParameterTable(ProjectEditor editor) {
		this.editor = editor;
		this.project = editor.getModel();
		variantNames = new String[project.getVariants().size()];
		for (int i = 0; i < variantNames.length; i++) {
			String name = project.getVariants().get(i).getName();
			variantNames[i] = name == null ? "" : name;
		}
		Arrays.sort(variantNames);
		initParameterRedefs();
	}

	private void initParameterRedefs() {
		for (ProjectVariant variant : project.getVariants()) {
			for (ParameterRedef redef : variant.getParameterRedefs()) {
				if (!contains(redef))
					redefs.add(redef);
			}
		}
		Collections.sort(redefs, new Comparator<ParameterRedef>() {
			@Override
			public int compare(ParameterRedef o1, ParameterRedef o2) {
				return Strings.compare(o1.getName(), o2.getName());
			}
		});
	}

	/**
	 * true if a parameter redefinition with the given name and process ID (can
	 * be null) exists.
	 */
	private boolean contains(ParameterRedef redef) {
		for (ParameterRedef contained : redefs) {
			if (Objects.equals(redef.getName(), contained.getName())
					&& Objects.equals(redef.getProcessId(),
							contained.getProcessId()))
				return true;
		}
		return false;
	}

	public void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] props = new String[variantNames.length + 2];
		props[0] = PARAMETER;
		props[1] = PROCESS;
		for (int i = 0; i < variantNames.length; i++)
			props[i + 2] = variantNames[i];
		viewer = Tables.createViewer(composite, props);
		viewer.setLabelProvider(new LabelProvider());
		double[] colWeights = new double[props.length];
		for (int i = 0; i < props.length; i++)
			colWeights[i] = 0.8 / props.length;
		Tables.bindColumnWidths(viewer, colWeights);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		viewer.setInput(redefs);
		setModifySupport();
	}

	private void setModifySupport() {
		modifySupport = new ModifySupport<>(viewer);
		for (String variantName : variantNames)
			modifySupport.bind(variantName, new ValueModifier(variantName));
	}

	private ParameterRedef findVariantRedef(String variantName,
			ParameterRedef redef) {
		ProjectVariant var = findVariant(variantName);
		if (var == null)
			return null;
		for (ParameterRedef variantRedef : var.getParameterRedefs()) {
			if (Objects.equals(variantRedef.getName(), redef.getName())
					&& Objects.equals(variantRedef.getProcessId(),
							redef.getProcessId()))
				return variantRedef;
		}
		return null;
	}

	private ProjectVariant findVariant(String variantName) {
		for (ProjectVariant variant : project.getVariants()) {
			if (Objects.equals(variantName, variant.getName()))
				return variant;
		}
		log.warn("could not find variant {}", variantName);
		return null;
	}

	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex != 0)
				return null;
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (redef.getProcessId() == null)
				return ImageType.FORMULA_ICON.get();
			else
				return ImageType.PROCESS_ICON.get();
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ParameterRedef))
				return null;
			ParameterRedef redef = (ParameterRedef) element;
			if (col == 0)
				return redef.getName();
			if (col == 1)
				return getProcessColumnText(redef);
			else
				return getVariantValue(col, redef);
		}

		private String getVariantValue(int col, ParameterRedef redef) {
			int idx = col - 2;
			if (idx < 0 || idx >= variantNames.length)
				return null;
			String variantName = variantNames[idx];
			ParameterRedef variantRedef = findVariantRedef(variantName, redef);
			if (variantRedef == null)
				return null;
			return Double.toString(variantRedef.getValue());
		}

		private String getProcessColumnText(ParameterRedef redef) {
			if (redef.getProcessId() == null)
				return "global";
			else {
				ProcessDescriptor descriptor = cache.get(
						ProcessDescriptor.class, redef.getProcessId());
				return Labels.getDisplayName(descriptor);
			}
		}
	}

	private class ValueModifier extends TextCellModifier<ParameterRedef> {

		private String variantName;

		public ValueModifier(String variantName) {
			this.variantName = variantName;
		}

		protected String getText(ParameterRedef redef) {
			ParameterRedef variantRedef = findVariantRedef(variantName, redef);
			if (variantRedef == null)
				return "";
			return Double.toString(variantRedef.getValue());
		}

		@Override
		protected void setText(ParameterRedef redef, String text) {
			if (redef == null || text == null)
				return;
			ParameterRedef variantRedef = findVariantRedef(variantName, redef);
			if (variantRedef == null) {
				ProjectVariant variant = findVariant(variantName);
				if (variant == null)
					return;
				variantRedef = redef.clone();
				variant.getParameterRedefs().add(variantRedef);
			}
			try {
				double d = Double.parseDouble(text);
				variantRedef.setValue(d);
				editor.setDirty(true);
			} catch (Exception e) {
			}
		}
	}

}
