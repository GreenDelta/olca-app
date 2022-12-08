package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPart;
import org.openlca.app.db.Database;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BuildAction extends WorkbenchPartAction {

	protected Map<Exchange, Process> mapExchangesToProcess;
	protected ProcessType preferredType;
	protected ProviderLinking providers;
	protected final FlowDao flowDao = new FlowDao(Database.get());
	;
	protected final ProcessDao processDao = new ProcessDao(Database.get());

	public BuildAction(IWorkbenchPart part) {
		super(part);
	}

	public void setMapExchangesToProcess(Map<Exchange, Process> mapExchangesToProcess) {
		this.mapExchangesToProcess = mapExchangesToProcess;
	}

	public void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
	}

	public void setProviderMethod(ProviderLinking providers) {
		this.providers = providers;
	}

	protected RootDescriptor findProvider(Exchange e) {
		if (e.flow == null)
			return null;
		if (providers == ProviderLinking.ONLY_DEFAULTS) {
			if (e.defaultProviderId == 0L)
				return null;
			return processDao.getDescriptor(e.defaultProviderId);
		}
		if (providers == ProviderLinking.PREFER_DEFAULTS
				&& e.defaultProviderId != 0L)
			return processDao.getDescriptor(e.defaultProviderId);

		ProcessDescriptor bestMatch = null;
		for (var d : getProviders(e)) {
			if (d.processType == preferredType)
				return d;
			if (bestMatch != null)
				continue;
			bestMatch = d;
		}
		return bestMatch;
	}

	protected List<ProcessDescriptor> getProviders(Exchange e) {
		if (e == null || e.flow == null)
			return Collections.emptyList();
		Set<Long> providerIds = e.isInput
				? flowDao.getWhereOutput(e.flow.id)
				: flowDao.getWhereInput(e.flow.id);
		return processDao.getDescriptors(providerIds);
	}

	protected ProcessLink getLink(Exchange exchange,
												 Process process, RootDescriptor provider) {
		var link = new ProcessLink();
		link.exchangeId = exchange.id;
		link.flowId = exchange.flow.id;
		link.processId = process.id;
		link.providerId = provider.id;
		if (provider.type == null)
			link.providerType = ProcessLink.ProviderType.PROCESS;
		else {
			link.providerType = switch (provider.type) {
				case PRODUCT_SYSTEM -> ProcessLink.ProviderType.SUB_SYSTEM;
				case RESULT -> ProcessLink.ProviderType.RESULT;
				default -> ProcessLink.ProviderType.PROCESS;
			};
		}
		return link;
	}

}
