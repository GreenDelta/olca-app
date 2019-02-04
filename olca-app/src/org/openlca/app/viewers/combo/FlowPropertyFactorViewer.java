package org.openlca.app.viewers.combo;

import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;

public class FlowPropertyFactorViewer extends
		AbstractComboViewer<FlowPropertyFactor> {

	public FlowPropertyFactorViewer(Composite parent) {
		super(parent);
		setInput(new FlowPropertyFactor[0]);
	}

	public void setInput(Flow flow) {
		List<FlowPropertyFactor> factors = flow.flowPropertyFactors;
		setInput(factors.toArray(new FlowPropertyFactor[factors.size()]));
	}

	@Override
	public Class<FlowPropertyFactor> getType() {
		return FlowPropertyFactor.class;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new FactorLabelProvider();
	}

	private class FactorLabelProvider extends BaseLabelProvider implements
			ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return Images.get(ModelType.FLOW_PROPERTY);
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof FlowPropertyFactor))
				return null;
			FlowPropertyFactor factor = (FlowPropertyFactor) element;
			return factor.flowProperty.name;
		}

	}

}
