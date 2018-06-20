package org.openlca.app.wizards;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
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
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.product.index.LinkingMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private TreeViewer processTree;
	private Process refProcess;
	private Button supplyChainCheck;
	private Button ignoreProvidersRadio;
	private Button preferProvidersRadio;
	private Button onlyLinkProvidersRadio;
	private Button preferUnitRadio;
	private Button preferSystemRadio;
	private Button cutoffCheck;
	private Text cutoffText;
	private Text filterText;

	ProductSystemWizardPage() {
		super("ProductSystemWizardPage");
		setTitle(M.NewProductSystem);
		setMessage(M.CreatesANewProductSystem);
		setPageComplete(false);
		setWithDescription(false);
	}

	void setProcess(Process process) {
		this.refProcess = process;
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
		supplyChainCheck = UI.checkBox(comp, M.AutoLinkProcesses);
		Composite methodGroup = createRadioGroup(comp, M.ProviderLinking);
		ignoreProvidersRadio = UI.formRadio(methodGroup, M.IgnoreDefaultProviders);
		preferProvidersRadio = UI.formRadio(methodGroup, M.PreferDefaultProviders);
		onlyLinkProvidersRadio = UI.formRadio(methodGroup, M.OnlyLinkDefaultProviders);
		Composite typeGroup = createRadioGroup(comp, M.PreferredProcessType);
		preferUnitRadio = UI.formRadio(typeGroup, Labels.processType(ProcessType.UNIT_PROCESS));
		preferSystemRadio = UI.formRadio(typeGroup, Labels.processType(ProcessType.LCI_RESULT));
		createCutoffText(comp);
		initialSelection();
		initializeChangeHandler();
	}

	private Composite createRadioGroup(Composite parent, String label) {
		UI.filler(parent);
		UI.formLabel(parent, label);
		UI.filler(parent);
		Composite group = UI.formComposite(parent);
		UI.gridLayout(group, 2, 10, 0).marginLeft = 10;
		return group;
	}

	private void createCutoffText(Composite comp) {
		UI.filler(comp);
		Composite inner = new Composite(comp, SWT.NONE);
		UI.gridLayout(inner, 2, 5, 0);
		cutoffCheck = UI.checkBox(inner, M.Cutoff);
		cutoffText = new Text(inner, SWT.BORDER);
		UI.gridData(cutoffText, true, false);
	}

	private void initialSelection() {
		supplyChainCheck.setSelection(true);
		preferProvidersRadio.setSelection(true);
		preferSystemRadio.setSelection(true);
		cutoffText.setEnabled(false);
	}

	private void initializeChangeHandler() {
		Controls.onSelect(supplyChainCheck, this::onAutoLinkChange);
		Controls.onSelect(ignoreProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(preferProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(onlyLinkProvidersRadio, this::onLinkingMethodChange);
		Controls.onSelect(cutoffCheck, e -> cutoffText.setEnabled(cutoffCheck.getSelection()));
	}

	private void onAutoLinkChange(SelectionEvent e) {
		boolean autolink = supplyChainCheck.getSelection();
		preferUnitRadio.setEnabled(autolink);
		preferSystemRadio.setEnabled(autolink);
		ignoreProvidersRadio.setEnabled(autolink);
		preferProvidersRadio.setEnabled(autolink);
		onlyLinkProvidersRadio.setEnabled(autolink);
		cutoffCheck.setEnabled(autolink);
		cutoffText.setEnabled(autolink);
	}

	private void onLinkingMethodChange(SelectionEvent e) {
		preferUnitRadio.setEnabled(!onlyLinkProvidersRadio.getSelection());
		preferSystemRadio.setEnabled(!onlyLinkProvidersRadio.getSelection());
	}

	@Override
	public ProductSystem createModel() {
		ProductSystem system = ProductSystem.from(refProcess);
		system.setName(getModelName());
		system.setDescription(getModelDescription());
		return system;
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null && !hasRefFlow()) {
			setErrorMessage(M.NoReferenceProcessSelected);
		}
		setPageComplete(getErrorMessage() == null);
	}

	private boolean hasRefFlow() {
		if (refProcess == null)
			return false;
		Exchange qRef = refProcess.getQuantitativeReference();
		if (qRef == null || qRef.flow == null)
			return false;
		FlowType type = qRef.flow.getFlowType();
		if (type == FlowType.PRODUCT_FLOW)
			return !qRef.isInput;
		if (type == FlowType.WASTE_FLOW)
			return qRef.isInput;
		return false;
	}

	boolean addSupplyChain() {
		return supplyChainCheck.getSelection();
	}

	ProcessType getPreferredType() {
		if (preferUnitRadio.getSelection())
			return ProcessType.UNIT_PROCESS;
		if (preferSystemRadio.getSelection())
			return ProcessType.LCI_RESULT;
		return ProcessType.LCI_RESULT;
	}

	LinkingMethod getLinkingMethod() {
		if (ignoreProvidersRadio.getSelection())
			return LinkingMethod.IGNORE_PROVIDERS;
		if (preferProvidersRadio.getSelection())
			return LinkingMethod.PREFER_PROVIDERS;
		if (onlyLinkProvidersRadio.getSelection())
			return LinkingMethod.ONLY_LINK_PROVIDERS;
		return LinkingMethod.ONLY_LINK_PROVIDERS;
	}

	Double getCutoff() {
		String s = cutoffText.getText();
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		try {
			double cutoff = Double.parseDouble(s);
			log.trace("Cutoff set to {}", cutoff);
			return cutoff;
		} catch (Exception ex) {
			log.warn("invalid number: cutoff {}", s);
			return null;
		}
	}

}
