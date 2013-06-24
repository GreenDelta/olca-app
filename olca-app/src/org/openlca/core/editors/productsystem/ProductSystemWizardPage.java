package org.openlca.core.editors.productsystem;

import java.util.UUID;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.core.application.FeatureFlag;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.viewer.ModelComponentTreeViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProductSystemWizardPage extends ModelWizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String EMPTY_REFERENCEPROCESS_ERROR = Messages.Systems_EmptyReferenceProcessError;

	private Button addSupplyChainButton;
	private TreeViewer processViewer;
	private Process selectedProcess;
	private Button useSystemProcesses;
	private double cutoff = 0;

	public ProductSystemWizardPage() {
		super("ProductSystemWizardPage");
		setTitle(Messages.Systems_WizardTitle);
		setMessage(Messages.Systems_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PRODUCT_SYSTEM.getDescriptor());
		setPageComplete(false);
	}

	public boolean addSupplyChain() {
		return addSupplyChainButton.getSelection();
	}

	@Override
	public Object[] getData() {
		final ProductSystem productSystem = new ProductSystem(UUID.randomUUID()
				.toString(), getComponentName());
		productSystem.setCategoryId(getCategoryId());
		productSystem.setDescription(getComponentDescription());

		try {
			// load reference process
			final Process process = getDatabase().select(Process.class,
					selectedProcess.getId());
			productSystem.add(process);
			productSystem.setReferenceProcess(process);
			if (process.getQuantitativeReference() != null) {
				productSystem.setReferenceExchange(process
						.getQuantitativeReference());
				productSystem.setTargetUnit(productSystem
						.getReferenceExchange().getUnit());
				productSystem.setTargetFlowPropertyFactor(productSystem
						.getReferenceExchange().getFlowPropertyFactor());
			} else {
				log.error("No quantitative reference on process '{}', "
						+ "calculation will fail.", process.getName());
			}
			productSystem.setTargetAmount(1);
		} catch (final Exception e) {
			log.error("Loading reference process failed", e);
		}
		return new Object[] { productSystem };
	}

	public boolean useSystemProcesses() {
		return useSystemProcesses.getSelection();
	}

	public double getCutoff() {
		return cutoff;
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null && selectedProcess == null) {
			setErrorMessage(EMPTY_REFERENCEPROCESS_ERROR);
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		new Label(container, SWT.TOP).setText(Messages.Common_ReferenceProcess);
		Composite c = new Composite(container, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createProcessViewer(c);
		createOptions(container);
	}

	private void createProcessViewer(Composite container) {
		NavigationRoot root = Navigator.getNavigationRoot();
		processViewer = new ModelComponentTreeViewer(container, false, false,
				root != null ? root.getCategoryRoot(Process.class,
						getDatabase()) : root, null);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		processViewer.getTree().setLayoutData(gd);
	}

	private void createOptions(final Composite container) {
		addSupplyChainButton = UIFactory.createButton(container,
				Messages.Systems_AddSupplyChain);
		addSupplyChainButton.setSelection(true);
		useSystemProcesses = UIFactory.createButton(container,
				Messages.Systems_UseSystemProcesses);
		useSystemProcesses.setSelection(true);
		if (FeatureFlag.PRODUCT_SYSTEM_CUTOFF.isEnabled()) {
			createCutoffText(container);
		}
	}

	private void createCutoffText(final Composite container) {
		final Text cutoffText = UI.formText(container, "Cut-off");
		cutoffText.setText("0.0");
		cutoffText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String text = cutoffText.getText();
				try {
					cutoff = Double.parseDouble(text);
					log.trace("Cutoff set to {}", cutoff);
				} catch (Exception ex) {
					log.warn("invalid number: cutoff {}", text);
				}
			}
		});
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();

		addSupplyChainButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				useSystemProcesses.setEnabled(addSupplyChainButton
						.getSelection());
			}
		});

		processViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						if (selection.getFirstElement() instanceof ModelElement) {
							final ModelElement elem = (ModelElement) ((IStructuredSelection) processViewer
									.getSelection()).getFirstElement();
							selectedProcess = (Process) elem.getData();
							checkInput();
						} else {
							selectedProcess = null;
							checkInput();
						}
					}
				});

	}

}
