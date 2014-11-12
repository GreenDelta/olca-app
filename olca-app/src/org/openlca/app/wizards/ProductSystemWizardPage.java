package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String EMPTY_REFERENCEPROCESS_ERROR = Messages.NoReferenceProcessSelected;

	private Button addSupplyChainButton;
	private TreeViewer processViewer;
	private Process refProcess;
	private Button useSystemProcesses;
	private double cutoff = 0;
	private Text filterText;

	public ProductSystemWizardPage() {
		super("ProductSystemWizardPage");
		setTitle(Messages.NewProductSystem);
		setMessage(Messages.CreatesANewProductSystem);
		setImageDescriptor(ImageType.NEW_WIZ_PRODUCT_SYSTEM.getDescriptor());
		setPageComplete(false);
	}

	public void setProcess(Process process) {
		this.refProcess = process;
	}

	public boolean addSupplyChain() {
		return addSupplyChainButton.getSelection();
	}

	@Override
	public ProductSystem createModel() {
		ProductSystem productSystem = new ProductSystem();
		productSystem.setRefId(UUID.randomUUID().toString());
		productSystem.setName(getModelName());
		productSystem.setDescription(getModelDescription());
		try {
			productSystem.getProcesses().add(refProcess.getId());
			productSystem.setReferenceProcess(refProcess);
			Exchange qRef = refProcess.getQuantitativeReference();
			productSystem.setReferenceExchange(qRef);
			productSystem.setTargetUnit(qRef.getUnit());
			productSystem.setTargetFlowPropertyFactor(qRef
					.getFlowPropertyFactor());
			productSystem.setTargetAmount(qRef.getAmountValue());
		} catch (final Exception e) {
			log.error("Loading reference process failed / no selected", e);
		}
		return productSystem;
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
		if (getErrorMessage() == null && refProcess == null) {
			setErrorMessage(EMPTY_REFERENCEPROCESS_ERROR);
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(Composite container) {
		filterText = UI.formText(container, Messages.Filter);
		UI.formLabel(container, Messages.ReferenceProcess);
		createProcessViewer(container);
		createOptions(container);
		if (refProcess != null) {
			nameText.setText(refProcess.getName());
			ProcessDescriptor descriptor = Descriptors.toDescriptor(refProcess);
			INavigationElement<?> elem = Navigator.findElement(descriptor);
			if (elem != null)
				processViewer.setSelection(new StructuredSelection(elem));
			checkInput();
		}
	}

	private void createProcessViewer(Composite container) {
		processViewer = NavigationTree.createViewer(container);
		processViewer.setInput(Navigator.findElement(ModelType.PROCESS));
		processViewer.addFilter(new EmptyCategoryFilter());
		processViewer.addFilter(new ModelTextFilter(filterText, processViewer));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		processViewer.getTree().setLayoutData(gd);
	}

	private void createOptions(final Composite container) {
		addSupplyChainButton = UIFactory.createButton(container,
				Messages.AddConnectedProcesses);
		addSupplyChainButton.setSelection(true);
		useSystemProcesses = UIFactory.createButton(container,
				Messages.ConnectWithSystemProcessesIfPossible);
		useSystemProcesses.setSelection(true);
		// if (FeatureFlag.PRODUCT_SYSTEM_CUTOFF.isEnabled()) {
		// createCutoffText(container);
		// }
	}

	private void createCutoffText(final Composite container) {
		final Text cutoffText = UI.formText(container, Messages.Cutoff);
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

		Controls.onSelect(addSupplyChainButton, (e) -> {
			useSystemProcesses.setEnabled(addSupplyChainButton
					.getSelection());
		});

		processViewer
				.addSelectionChangedListener((event) -> {
					IStructuredSelection selection = (IStructuredSelection) event
							.getSelection();
					if (selection.getFirstElement() instanceof ModelElement) {
						ModelElement elem = (ModelElement) ((IStructuredSelection) processViewer
								.getSelection()).getFirstElement();
						try {
							refProcess = Database.load(elem.getContent());
							checkInput();
						} catch (Exception e) {
							log.error("failed to load process", e);
						}
					} else {
						refProcess = null;
						checkInput();
					}
				});
	}

}
