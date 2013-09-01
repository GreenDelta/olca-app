package org.openlca.app.inventory;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.results.InventoryResultPage;
import org.openlca.app.results.InventoryResultProvider;
import org.openlca.core.database.Cache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.InventoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryResultEditor extends FormEditor {

	public static String ID = "InventoryResultEditor";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private InventoryResult result;

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		try {
			InventoryResultInput input = (InventoryResultInput) editorInput;
			setup = App.getCache().get(input.getSetupKey(),
					CalculationSetup.class);
			result = App.getCache().get(input.getResultKey(),
					InventoryResult.class);
		} catch (Exception e) {
			log.error("failed to load inventory result", e);
			throw new PartInitException("failed to load inventory result", e);
		}
	}

	public CalculationSetup getSetup() {
		return setup;
	}

	public InventoryResult getResult() {
		return result;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InventoryResultPage(this, new InventoryAdapter()));
		} catch (Exception e) {
			log.error("failed to add pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		// TODO: save result as system process
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false; // result != null;
	}

	@Override
	public void setFocus() {
	}

	private class InventoryAdapter implements InventoryResultProvider {

		@Override
		public Collection<FlowDescriptor> getFlows(Cache cache) {
			return result.getFlowResults().getFlows(cache);
		}

		@Override
		public double getAmount(FlowDescriptor flow) {
			return result.getFlowResult(flow.getId());
		}

		@Override
		public boolean isInput(FlowDescriptor flow) {
			return result.getFlowIndex().isInput(flow.getId());
		}
	}

}
