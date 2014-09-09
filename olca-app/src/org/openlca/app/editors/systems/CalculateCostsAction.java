package org.openlca.app.editors.systems;

import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;

public class CalculateCostsAction extends Action {

	private ProductSystemEditor editor;

	public CalculateCostsAction() {
		setToolTipText(Messages.CalculateCosts);
		setImageDescriptor(ImageType.COST_CALC_ICON.getDescriptor());
	}

	public void setActiveEditor(ProductSystemEditor editor) {
		this.editor = editor;
	}

	@Override
	public void run() {
		if (editor == null)
			return;
		// final ProductSystem system = (ProductSystem)
		// editor.getModelComponent();
		// final CostCalculator costCalculator = new CostCalculator(
		// Cache.getMatrixCache());
		// BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
		// @Override
		// public void run() {
		// CostResult costResult = costCalculator.calculate(system);
		// CostResultEditorInput input = new CostResultEditorInput(system,
		// costResult);
		// Editors.open(input, CostResultEditor.ID);
		// }
		// });
	}

}
