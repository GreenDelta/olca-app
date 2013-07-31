package org.openlca.app.viewer;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.core.model.AllocationMethod;

public class AllocationMethodViewer extends
		AbstractComboViewer<AllocationMethod> {

	public static final int APPEND_INHERIT_OPTION = 1;

	public AllocationMethodViewer(Composite parent) {
		this(parent, -1);
	}

	public AllocationMethodViewer(Composite parent, int flag) {
		super(parent);
		if (flag == APPEND_INHERIT_OPTION) {
			AllocationMethod[] values = new AllocationMethod[AllocationMethod
					.values().length + 1];
			for (int i = 0; i < AllocationMethod.values().length; i++)
				values[i + 1] = AllocationMethod.values()[i];
			setInput(values);
			select(null);
		} else {
			setInput(AllocationMethod.values());
			select(AllocationMethod.None);
		}
	}

	@Override
	public void select(AllocationMethod value) {
		if (value == null)
			if (getInput().length == AllocationMethod.values().length)
				super.select(AllocationMethod.None);
			else
				((TableCombo) getViewer().getControl()).select(0);
		else
			super.select(value);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new AllocationMethodLabelProvider();
	}

	private class AllocationMethodLabelProvider extends BaseLabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof AbstractViewer.Null)
				return Messages.AsDefinedInProcesses;
			return super.getText(element);
		}

	}
}
