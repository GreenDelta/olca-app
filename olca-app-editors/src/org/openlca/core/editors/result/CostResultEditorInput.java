package org.openlca.core.editors.result;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.results.SimpleCostResult;
import org.openlca.core.resources.ImageType;
import org.openlca.util.Strings;

/** Editor input for cost results. */
public class CostResultEditorInput implements IEditorInput {

	private ProductSystem system;
	private SimpleCostResult result;

	public CostResultEditorInput(ProductSystem system, SimpleCostResult result) {
		this.system = system;
		this.result = result;
	}

	public SimpleCostResult getResult() {
		return result;
	}

	@Override
	public boolean exists() {
		return result != null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.COST_CALC_ICON.getDescriptor();
	}

	@Override
	public String getName() {
		return Strings.cut("Cost result of " + system.getName(), 50);
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Cost result of " + system.getName();
	}

}
