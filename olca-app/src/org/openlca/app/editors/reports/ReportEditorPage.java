package org.openlca.app.editors.reports;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportParameter;
import org.openlca.app.editors.reports.model.ReportVariant;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class ReportEditorPage extends FormPage {

	private Report report;
	private ReportEditor editor;
	private DataBinding binding;
	private EntityCache cache = Cache.getEntityCache();

	private FormToolkit toolkit;
	private SectionList sectionList;

	public ReportEditorPage(ReportEditor editor, Report report) {
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
		createVariantsSection(body);
		// TODO: do we need parameters here?
		// createParameternamesSection(body);
		createAddButton(body);
		sectionList = new SectionList(editor, body, form, toolkit);
		form.reflow(true);
	}

	private void createAddButton(Composite body) {
		Composite composite = UI.formComposite(body, toolkit);
		UI.formLabel(composite, "");
		Button addButton = toolkit.createButton(composite, "Add section",
				SWT.NONE);
		addButton.setImage(ImageType.ADD_ICON.get());
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sectionList.addNew();
			}
		});
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		Text titleText = UI.formText(composite, toolkit, "Title");
		binding.onString(() -> report, "title", titleText);
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

	private void createVariantsSection(Composite body) {
		Section section = UI.section(body, toolkit, "Variants");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		TableViewer viewer = Tables.createViewer(composite, Messages.Name,
				Messages.UserFriendlyName, Messages.Description);
		viewer.setLabelProvider(new VariantLabel());
		Tables.bindColumnWidths(viewer, 0.30, 0.30, 0.40);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		ModifySupport<ReportVariant> modifier = new ModifySupport<>(viewer);
		modifier.bind(Messages.UserFriendlyName, new VariantNameModifier());
		modifier.bind(Messages.Description, new VariantDescriptionModifier());
		viewer.setInput(report.getVariants());
	}

	private void createParameternamesSection(Composite body) {
		Section section = UI.section(body, toolkit, Messages.ReportParameters);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Parameter, Messages.Context,
				Messages.UserFriendlyName, Messages.Value, Messages.Description };
		TableViewer viewer = Tables.createViewer(composite, properties);
		viewer.setLabelProvider(new ParameterLabel());
		Tables.bindColumnWidths(viewer, 0.20, 0.20, 0.20, 0.20, 0.20);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
		viewer.setInput(report.getParameters());
		ModifySupport<ReportParameter> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(Messages.UserFriendlyName,
				new ParameterNameModifier());
		modifySupport.bind(Messages.Description,
				new ParameterDescriptionModifier());
	}

	private class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
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

	private class VariantLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ReportVariant))
				return null;
			ReportVariant variant = (ReportVariant) element;
			switch (col) {
			case 0:
				return variant.getName();
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