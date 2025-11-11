package org.openlca.app.editors.graphical.actions;

import java.util.Map;

import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;

interface IBuildAction {

	void setExchanges(Map<Exchange, Process> exchanges);

	void setPreferredType(ProcessType preferredType);

	void setProviderMethod(ProviderLinking providerLinking);

	String getText();

	void run();

}
