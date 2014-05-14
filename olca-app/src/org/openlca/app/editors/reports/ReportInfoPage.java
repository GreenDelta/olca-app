package org.openlca.app.editors.reports;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.editors.DataBinding.TextBindType;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class ReportInfoPage extends FormPage {

	private Report report;
	private ReportEditor editor;
	private DataBinding binding;
	private EntityCache cache = Cache.getEntityCache();

	private FormToolkit toolkit;

	public ReportInfoPage(ReportEditor editor, Report report) {
		super(editor, "ReportInfoPage", "Report");
		this.editor = editor;
		this.report = report;
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Report for project: "
				+ Labels.getDisplayName(report.getProject()));
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		for (ReportSection reportSection : report.getSections())
			createSection(reportSection, body);
		createParameternamesSection(body);
		createVariantsSection(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		Text titleText = UI.formText(composite, toolkit, "Title");
		binding.on(report, "title", DataBinding.TextBindType.STRING, titleText);
		UI.formLabel(composite, toolkit, Messages.Project);
		ImageHyperlink link = toolkit.createImageHyperlink(composite, SWT.TOP);
		link.setText(Labels.getDisplayName(report.getProject()));
		link.setForeground(Colors.getLinkBlue());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				App.openEditor(report.getProject());
			}
		});
		createPreviewButton(composite);
	}

	private void createPreviewButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, "Preview", SWT.NONE);
		button.setImage(ImageType.SEARCH_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ReportViewer.open(report);
			}
		});
	}

	private void createSection(ReportSection reportSection, Composite body) {
		final Section section = UI.section(body, toolkit,
				reportSection.getTitle());
		Composite composite = UI.sectionClient(section, toolkit);
		final Text titleText = UI.formText(composite, toolkit, "Section");
		binding.on(reportSection, "title", TextBindType.STRING, titleText);
		titleText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				section.setText(titleText.getText());
			}
		});
		Text descriptionText = UI.formMultiText(composite, toolkit, "Text");
		binding.on(reportSection, "text", TextBindType.STRING, descriptionText);
		createComponentViewer(reportSection, composite);
	}

	private void createComponentViewer(final ReportSection reportSection,
			Composite composite) {
		UI.formLabel(composite, toolkit, "Component");
		ComboViewer viewer = new ComboViewer(composite);
		UI.gridData(viewer.getControl(), false, false).widthHint = 250;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new ComponentLabel());
		viewer.setInput(ReportComponent.values());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent evt) {
				ReportComponent component = Viewers.getFirst(evt.getSelection());
				if (component == null || component == ReportComponent.NONE)
					reportSection.setComponentId(null);
				else
					reportSection.setComponentId(component.getId());
				editor.setDirty(true);
			}
		});
	}

	private void createReportParameters() {
		report.getParameters().clear();
		for (ParameterRedef redef : report.getProject().getVariants().get(0)
				.getParameterRedefs()) {
			ReportParameter parameter = new ReportParameter();
			parameter.setRedef(redef);
			parameter.setValue(redef.getValue());
			parameter.setDefaultValue(redef.getValue());
			report.getParameters().add(parameter);
		}
	}

	private void createParameternamesSection(Composite body) {
		Section section = UI.section(body, toolkit, Messages.ReportParameters);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Parameter, Messages.Context,
				Messages.UserFriendlyName, Messages.Value, Messages.Description };
		TableViewer viewer = Tables.createViewer(composite, properties);
		viewer.setLabelProvider(new ParametersTableLabel());
		Tables.bindColumnWidths(viewer, 0.20, 0.20, 0.20, 0.20, 0.20);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		createReportParameters();
		viewer.setInput(report.getParameters());
		ModifySupport<ReportParameter> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(Messages.UserFriendlyName,
				new ParameterNameModifier());
		modifySupport.bind(Messages.Description,
				new ParameterDescriptionModifier());
	}

	private void createReportVariants() {
		report.getVariants().clear();
		for (ProjectVariant projectVariant : report.getProject().getVariants()) {
			ReportVariant variant = new ReportVariant();
			variant.setVariant(projectVariant);
			report.getVariants().add(variant);
		}
	}

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, "Variants");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Name, Messages.UserFriendlyName,
				Messages.Description };
		TableViewer viewer;
		viewer = Tables.createViewer(composite, properties);
		viewer.setLabelProvider(new VariantLabelProvider());
		Tables.bindColumnWidths(viewer, 0.30, 0.30, 0.40);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		createReportVariants();
		viewer.setInput(report.getVariants());
		ModifySupport<ReportVariant> modifySupport = new ModifySupport<>(viewer);
		modifySupport
				.bind(Messages.UserFriendlyName, new VariantNameModifier());
		modifySupport.bind(Messages.Description,
				new VariantDescriptionModifier());
	}

	private class ComponentLabel extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (!(element instanceof ReportComponent))
				return null;
			ReportComponent component = (ReportComponent) element;
			return getLabel(component);
		}

		private String getLabel(ReportComponent component) {
			switch (component) {
			case NONE:
				return "None";
			case PARAMETER_TABLE:
				return "Parameter table";
			case RESULT_CHART:
				return "Result chart";
			case RESULT_TABLE:
				return "Result table";
			case VARIANT_TABLE:
				return "Project variant table";
			default:
				return "unknown";
			}
		}
	}

	private class ParametersTableLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0)
				return ImageType.PRODUCT_SYSTEM_ICON.get();
			else
				return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ReportParameter))
				return null;
			ReportParameter parameter = (ReportParameter) element;
			switch (col) {
			case 0:
				return parameter.getRedef().getName();
			case 1:
				if (parameter.getRedef() == null)
					return "";
				return getModelColumnText(parameter.getRedef());
			case 2:
				return parameter.getUserFriendlyName();
			case 3:
				return Double.toString(parameter.getValue());
			case 4:
				return parameter.getDescription();
			default:
				return null;
			}
		}

		private String getModelColumnText(ParameterRedef redef) {
			BaseDescriptor model = getModel(redef);
			if (model == null)
				return "global";
			else
				return Labels.getDisplayName(model);
		}

		private BaseDescriptor getModel(ParameterRedef redef) {
			if (redef == null || redef.getContextId() == null)
				return null;
			long modelId = redef.getContextId();
			BaseDescriptor model = cache.get(ImpactMethodDescriptor.class,
					modelId);
			if (model != null)
				return model;
			else
				return cache.get(ProcessDescriptor.class, modelId);
		}

	}

	private class ParameterNameModifier extends
			TextCellModifier<ReportParameter> {

		@Override
		protected String getText(ReportParameter parameter) {
			return parameter.getUserFriendlyName();
		}

		@Override
		protected void setText(ReportParameter parameter, String text) {
			if (text == null)
				return;
			if (!text.equals(parameter.getUserFriendlyName())) {
				parameter.setUserFriendlyName(text);
				editor.setDirty(true);
			}
		}
	}

	private class ParameterDescriptionModifier extends
			TextCellModifier<ReportParameter> {

		@Override
		protected String getText(ReportParameter parameter) {
			return parameter.getDescription();
		}

		@Override
		protected void setText(ReportParameter parameter, String text) {
			if (text == null)
				return;
			if (!text.equals(parameter.getDescription())) {
				parameter.setDescription(text);
				editor.setDirty(true);
			}
		}
	}

	private class VariantLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ReportVariant))
				return null;
			ReportVariant variant = (ReportVariant) element;
			ProductSystem system = variant.getVariant().getProductSystem();
			if (system == null)
				return null;
			switch (columnIndex) {
			case 0:
				return variant.getVariant().getName();
			case 1:
				return variant.getUserFriendlyName();
			case 2:
				return variant.getDescription();
			default:
				return null;
			}
		}

	}

	private class VariantNameModifier extends TextCellModifier<ReportVariant> {

		@Override
		protected String getText(ReportVariant variant) {
			return variant.getUserFriendlyName();
		}

		@Override
		protected void setText(ReportVariant variant, String text) {
			if (text == null)
				return;
			if (!text.equals(variant.getUserFriendlyName())) {
				variant.setUserFriendlyName(text);
				editor.setDirty(true);
			}
		}
	}

	private class VariantDescriptionModifier extends
			TextCellModifier<ReportVariant> {

		@Override
		protected String getText(ReportVariant variant) {
			return variant.getDescription();
		}

		@Override
		protected void setText(ReportVariant variant, String text) {
			if (text == null)
				return;
			if (!text.equals(variant.getDescription())) {
				variant.setDescription(text);
				editor.setDirty(true);
			}
		}
	}

}