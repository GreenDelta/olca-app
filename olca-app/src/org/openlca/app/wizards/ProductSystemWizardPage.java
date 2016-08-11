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
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String EMPTY_REFERENCEPROCESS_ERROR = M.NoReferenceProcessSelected;

	private Button supplyChainCheck;
	private TreeViewer processTree;
	private Process refProcess;
	private Button systemProcessesCheck;
	private Text filterText;

	Double cutoff;

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
		return supplyChainCheck.getSelection();
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
		return systemProcessesCheck.getSelection();
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
	protected void createContents(Composite comp) {
		filterText = UI.formText(comp, M.ReferenceProcess);
		UI.filler(comp);
		createProcessTree(comp);
		createOptions(comp);
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

	private void createOptions(Composite comp) {
		UI.filler(comp);
		supplyChainCheck = UI.checkBox(comp, M.AddConnectedProcesses);
		supplyChainCheck.setSelection(true);
		UI.filler(comp);
		systemProcessesCheck = UI.checkBox(comp,
				M.ConnectWithSystemProcessesIfPossible);
		systemProcessesCheck.setSelection(true);
		Controls.onSelect(supplyChainCheck, e -> {
			systemProcessesCheck.setEnabled(supplyChainCheck.getSelection());
		});
		createCutoffText(comp);
	}

	private void createCutoffText(Composite comp) {
		UI.filler(comp);
		Composite inner = new Composite(comp, SWT.NONE);
		UI.gridLayout(inner, 2, 5, 0);
		Button check = UI.checkBox(inner, M.Cutoff);
		Text text = new Text(inner, SWT.BORDER);
		text.setEnabled(false);
		UI.gridData(text, true, false);
		Controls.onSelect(check, e -> text.setEnabled(check.getSelection()));
		text.addModifyListener(e -> {
			String s = text.getText();
			if (Strings.isNullOrEmpty(s)) {
				cutoff = null;
				return;
			}
			try {
				cutoff = Double.parseDouble(s);
				log.trace("Cutoff set to {}", cutoff);
			} catch (Exception ex) {
				log.warn("invalid number: cutoff {}", s);
			}
		});
	}

}
