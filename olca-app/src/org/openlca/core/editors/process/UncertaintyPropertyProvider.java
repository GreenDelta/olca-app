package org.openlca.core.editors.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.openlca.app.Messages;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Expression;
import org.openlca.core.model.UncertaintyDistributionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UncertaintyPropertyProvider {

	private final String AMOUNT_PREF = "uncert_amount_";
	private final String PARAM_1_PREF = "uncert_1_";
	private final String PARAM_2_PREF = "uncert_2_";
	private final String PARAM_3_PREF = "uncert_3_";

	private Logger log = LoggerFactory.getLogger(getClass());
	private Exchange exchange;

	public UncertaintyPropertyProvider(Exchange exchange) {
		this.exchange = exchange;
	}

	public List<IPropertyDescriptor> getDescriptors() {
		if (exchange == null)
			return Collections.emptyList();
		List<IPropertyDescriptor> descriptors = new ArrayList<>();
		UncertaintyDistributionType type = exchange.getDistributionType();
		if (type == null || type == UncertaintyDistributionType.NONE)
			addProperty(AMOUNT_PREF, Messages.Processes_ResultingAmount,
					descriptors);
		else
			addDistributionDescriptors(type, descriptors);
		return descriptors;
	}

	public boolean hasPropertyValue(Object propertyId) {
		if (propertyId == null)
			return false;
		String[] prefixes = { AMOUNT_PREF, PARAM_1_PREF, PARAM_2_PREF,
				PARAM_3_PREF };
		for (String prefix : prefixes) {
			if (propertyId.toString().startsWith(prefix))
				return true;
		}
		return false;
	}

	public Object getPropertyValue(Object propertyId) {
		Expression expression = findExpression(propertyId);
		if (expression == null)
			return null;
		return expression.getFormula();
	}

	public void setPropertyValue(Object propertyId, Object value) {
		Expression expression = findExpression(propertyId);
		if (value != null) {
			expression.setFormula(value.toString());
		}
	}

	private Expression findExpression(Object propertyId) {
		if (propertyId == null || exchange == null)
			return null;
		String propString = propertyId.toString();
		if (propString.startsWith(AMOUNT_PREF))
			return exchange.getResultingAmount();
		if (propString.startsWith(PARAM_1_PREF))
			return exchange.getUncertaintyParameter1();
		if (propString.startsWith(PARAM_2_PREF))
			return exchange.getUncertaintyParameter2();
		if (propString.startsWith(PARAM_3_PREF))
			return exchange.getUncertaintyParameter3();
		return null;
	}

	private void addDistributionDescriptors(UncertaintyDistributionType type,
			List<IPropertyDescriptor> descriptors) {
		if (exchange.getUncertaintyParameter1() != null)
			addProperty(PARAM_1_PREF, firstUncertaintyLabel(type), descriptors);
		if (exchange.getUncertaintyParameter2() != null)
			addProperty(PARAM_2_PREF, secondUncertaintyLabel(type), descriptors);
		if (exchange.getUncertaintyParameter3() != null)
			addProperty(PARAM_3_PREF, thirdUncertaintyLabel(type), descriptors);
	}

	private void addProperty(String prefix, String label,
			List<IPropertyDescriptor> descriptors) {
		if (prefix == null || label == null)
			return;
		String id = prefix + exchange.getId();
		TextPropertyDescriptor d = new TextPropertyDescriptor(id, label);
		d.setCategory(Messages.Processes_DistributionValues);
		descriptors.add(d);
	}

	private String firstUncertaintyLabel(UncertaintyDistributionType type) {
		switch (type) {
		case LOG_NORMAL:
			return Messages.Common_GeometricMean;
		case NORMAL:
			return Messages.Common_Mean;
		case TRIANGLE:
			return Messages.Common_Minimum;
		case UNIFORM:
			return Messages.Common_Minimum;
		default:
			log.warn("Unknown label for uncertainty {}", type);
			return null;
		}
	}

	private String secondUncertaintyLabel(UncertaintyDistributionType type) {
		switch (type) {
		case LOG_NORMAL:
			return Messages.Common_GeometricStandardDeviation;
		case NORMAL:
			return Messages.Common_StandardDeviation;
		case TRIANGLE:
			return Messages.Common_Mode;
		case UNIFORM:
			return Messages.Common_Maximum;
		default:
			log.warn("Unknown label for uncertainty {}", type);
			return null;
		}
	}

	private String thirdUncertaintyLabel(UncertaintyDistributionType type) {
		switch (type) {
		case TRIANGLE:
			return Messages.Common_Maximum;
		default:
			log.warn("Unknown label for uncertainty {}", type);
			return null;
		}
	}

}
