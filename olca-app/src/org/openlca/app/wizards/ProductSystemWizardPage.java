package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.ProcessDao;
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

	private final String EMPTY_REFERENCEPROCESS_ERROR = M.NoReferenceProcessSelected;

	private Button addSupplyChainButton;
	private TreeViewer processTree;
	private Process refProcess;
	private Button useSystemProcesses;
	private double cutoff = 0;
	private Text filterText;

	public ProductSystemWizardPage() {
		super("ProductSystemWizardPage");
		setTitle(M.NewProductSystem);
		setMessage(M.CreatesANewProductSystem);
		setPageComplete(false);
		setWithDescription(false);
	}

	public void setProcess(Process process) {
		this.refProcess = process;
	}

	public boolean addSupplyChain() {
		return addSupplyChainButton.getSelection();
	}

	@Override
	public ProductSystem createModel() {
		ProductSystem system = new ProductSystem();
		system.setRefId(UUID.randomUUID().toString());
		system.setName(getModelName());
		system.setDescription(getModelDescription());
		try {
			system.getProcesses().add(refProcess.getId());
			system.setReferenceProcess(refProcess);
			Exchange qRef = refProcess.getQuantitativeReference();
			system.setReferenceExchange(qRef);
			system.setTargetUnit(qRef.getUnit());
			system.setTargetFlowPropertyFactor(qRef
					.getFlowPropertyFactor());
			system.setTargetAmount(qRef.getAmountValue());
		} catch (final Exception e) {
			log.error("Loading reference process failed / no selected", e);
		}
		return system;
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
	protected void createContents(Composite composite) {
		filterText = UI.formText(composite, M.ReferenceProcess);
		UI.formLabel(composite, "");
		createProcessTree(composite);
		createOptions(composite);
		if (refProcess != null) {
			nameText.setText(refProcess.getName());
			ProcessDescriptor descriptor = Descriptors.toDescriptor(refProcess);
			INavigationElement<?> elem = Navigator.findElement(descriptor);
			if (elem != null)
				processTree.setSelection(new StructuredSelection(elem));
			checkInput();
		}
	}

	private void createProcessTree(Composite composite) {
		processTree = NavigationTree.createViewer(composite);
		processTree.setInput(Navigator.findElement(ModelType.PROCESS));
		processTree.addFilter(new EmptyCategoryFilter());
		processTree.addFilter(new ModelTextFilter(filterText, processTree));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		processTree.getTree().setLayoutData(gd);
		processTree.addSelectionChangedListener(this::processSelected);
	}

	private void processSelected(SelectionChangedEvent e) {
		Object obj = Viewers.getFirst(e.getSelection());
		if (!(obj instanceof ModelElement)) {
			refProcess = null;
			checkInput();
			return;
		}
		ModelElement elem = (ModelElement) obj;
		try {
			ProcessDao dao = new ProcessDao(Database.get());
			refProcess = dao.getForId(elem.getContent().getId());
			checkInput();
		} catch (Exception ex) {
			log.error("failed to load process", ex);
		}
	}

	private void createOptions(Composite composite) {
		addSupplyChainButton = UIFactory.createButton(composite,
				M.AddConnectedProcesses);
		addSupplyChainButton.setSelection(true);
		useSystemProcesses = UIFactory.createButton(composite,
				M.ConnectWithSystemProcessesIfPossible);
		useSystemProcesses.setSelection(true);
		Controls.onSelect(addSupplyChainButton, (e) -> {
			useSystemProcesses.setEnabled(addSupplyChainButton.getSelection());
		});
		if (FeatureFlag.PRODUCT_SYSTEM_CUTOFF.isEnabled()) {
			createCutoffText(composite);
		}
	}

	private void createCutoffText(Composite composite) {
		Text text = UI.formText(composite, M.Cutoff);
		text.setText("0.0");
		text.addModifyListener(e -> {
			String s = text.getText();
			try {
				cutoff = Double.parseDouble(s);
				log.trace("Cutoff set to {}", cutoff);
			} catch (Exception ex) {
				log.warn("invalid number: cutoff {}", s);
			}
		});
	}

}
