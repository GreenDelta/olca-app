package org.openlca.app.analysis.sankey;

import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.BaseNameSorter;
import org.openlca.core.editors.ToolTipComboViewer;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * A dialog for selecting the flow/LCIA category and the cutoff to analyze in
 * the sankey diagram
 * 
 * @author Sebastian Greve
 * 
 */
public class SankeySelectionDialog extends FormDialog {

	private Set<ImpactCategoryDescriptor> categories;
	private double cutoff = 0.1;
	private Spinner cutoffSpinner;
	private Object firstSelection;
	private Button flowRadioButton;
	private Set<FlowDescriptor> flows;
	private Button lciaCategoryRadioButton;
	private ToolTipComboViewer selectionViewer;

	public SankeySelectionDialog(Set<FlowDescriptor> flows,
			Set<ImpactCategoryDescriptor> categories) {
		super(UI.shell());
		this.flows = flows;
		this.categories = categories;
	}

	/**
	 * Creates the contents of the first layer option composite
	 * 
	 * @param parent
	 *            The parent composite
	 * @param tookit
	 *            The form toolkit
	 */
	private void createFirstLayerComposite(final Composite parent,
			final FormToolkit tookit) {
		final Composite firstLayer = new Composite(parent, SWT.NONE);
		firstLayer.setLayout(new GridLayout());
		firstLayer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tookit.adapt(firstLayer);

		final Composite radioComposite = new Composite(firstLayer, SWT.NONE);
		radioComposite.setLayout(new GridLayout(2, false));
		radioComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		tookit.adapt(radioComposite);
		flowRadioButton = tookit.createButton(radioComposite, Messages.Flow,
				SWT.RADIO);
		flowRadioButton.setSelection(firstSelection == null
				|| firstSelection instanceof Flow);
		tookit.paintBordersFor(radioComposite);
		if (categories.size() > 0) {
			lciaCategoryRadioButton = tookit.createButton(radioComposite,
					Messages.ImpactCategory, SWT.RADIO);
			lciaCategoryRadioButton.setSelection(firstSelection != null
					&& firstSelection instanceof ImpactCategory);
		}
		selectionViewer = new ToolTipComboViewer(firstLayer, SWT.NONE);
		tookit.adapt(selectionViewer);
		selectionViewer.setLabelProvider(new BaseLabelProvider());
		selectionViewer.setContentProvider(ArrayContentProvider.getInstance());
		selectionViewer.setSorter(new BaseNameSorter());
		selectionViewer.setInput(firstSelection == null
				|| firstSelection instanceof FlowDescriptor ? flows
				: categories);
		selectionViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		if (firstSelection != null) {
			selectionViewer
					.setSelection(new StructuredSelection(firstSelection));
		}
		tookit.paintBordersFor(selectionViewer);

		tookit.paintBordersFor(firstLayer);
	}

	/**
	 * Initializes the listeners
	 */
	private void initListeners() {
		flowRadioButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				firstSelection = null;
				selectionViewer.setInput(flows);
				selectionViewer.setSelection(new StructuredSelection());
				getButton(OK).setEnabled(false);
			}
		});
		if (lciaCategoryRadioButton != null) {
			lciaCategoryRadioButton
					.addSelectionListener(new SelectionListener() {

						@Override
						public void widgetDefaultSelected(final SelectionEvent e) {
							// no action on default selection
						}

						@Override
						public void widgetSelected(final SelectionEvent e) {
							firstSelection = null;
							selectionViewer.setInput(categories);
							selectionViewer
									.setSelection(new StructuredSelection());
							getButton(OK).setEnabled(false);
						}
					});
		}

		selectionViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						firstSelection = ((IStructuredSelection) selectionViewer
								.getSelection()).getFirstElement();
						getButton(OK).setEnabled(
								!selectionViewer.getSelection().isEmpty());
					}
				});

		cutoffSpinner.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				cutoff = cutoffSpinner.getSelection() / 10000d;
			}
		});
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Control contents = super.createContents(parent);
		if (firstSelection == null) {
			getButton(OK).setEnabled(false);
		}
		return contents;
	}

	@Override
	protected void createFormContent(final IManagedForm mform) {
		final ScrolledForm form = mform.getForm();
		final FormToolkit toolkit = mform.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);

		form.setText(Messages.Sankey_DialogDescription);
		toolkit.decorateFormHeading(form.getForm());

		final Composite body = UI.formBody(form, toolkit);
		// create composite for first layer (relates to the connection links)
		createFirstLayerComposite(body, toolkit);

		// create the cutoff spinner
		final Composite cutoffComposite = new Composite(body, SWT.NONE);
		cutoffComposite.setLayout(new GridLayout(2, false));
		cutoffComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		toolkit.adapt(cutoffComposite);

		toolkit.createLabel(cutoffComposite, Messages.Sankey_Cutoff);
		cutoffSpinner = new Spinner(cutoffComposite, SWT.BORDER);
		cutoffSpinner.setIncrement(10);
		cutoffSpinner.setMinimum(0);
		cutoffSpinner.setMaximum(10000);
		cutoffSpinner.setDigits(2);
		cutoffSpinner.setSelection((int) (cutoff * 10000));
		toolkit.adapt(cutoffSpinner);

		toolkit.paintBordersFor(cutoffComposite);
		initListeners();
	}

	public double getCutoff() {
		return cutoff;
	}

	public Object getSelection() {
		return firstSelection;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public void setSelection(Object selection) {
		this.firstSelection = selection;
	}

}
