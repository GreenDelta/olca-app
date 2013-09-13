package org.openlca.app.systems;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.core.editors.result.CostResultEditor;
import org.openlca.core.editors.result.CostResultEditorInput;
import org.openlca.core.math.CostCalculator;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.results.SimpleCostResult;

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
		final ProductSystem system = (ProductSystem) editor.getModelComponent();
		final CostCalculator costCalculator = new CostCalculator(
				editor.getDatabase());
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {
				SimpleCostResult costResult = costCalculator.calculate(system);
				CostResultEditorInput input = new CostResultEditorInput(system,
						costResult);
				Editors.open(input, CostResultEditor.ID);
			}
		});
	}

}
