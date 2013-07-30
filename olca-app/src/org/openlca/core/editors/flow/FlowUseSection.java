package org.openlca.core.editors.flow;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Colors;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the section with links to providers and recipients of a given flow.
 */
class FlowUseSection {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Flow flow;
	private IDatabase database;
	private Composite parent;
	private FormToolkit toolkit;

	public FlowUseSection(Flow flow, IDatabase database) {
		this.flow = flow;
		this.database = database;
	}

	public void render(Composite body, FormToolkit toolkit) {
		log.trace("render flow-use-section for flow {}", flow);
		List<BaseDescriptor> recipients = getProcesses(true);
		List<BaseDescriptor> providers = getProcesses(false);
		if (recipients.isEmpty() && providers.isEmpty())
			return;
		Section section = UI.section(body, toolkit,
				Messages.Flows_UsedInProcesses);
		section.setExpanded(false);
		parent = UI.sectionClient(section, toolkit);
		this.toolkit = toolkit;
		renderLinks(Messages.Flows_ConsumedBy, recipients,
				ImageType.INPUT_ICON.get());
		renderLinks(Messages.Flows_ProducedBy, providers,
				ImageType.OUTPUT_ICON.get());
	}

	private List<BaseDescriptor> getProcesses(boolean forInput) {
		try {
			FlowDao dao = new FlowDao(database);
			if (forInput)
				return dao.getRecipients(flow);
			return dao.getProviders(flow);
		} catch (Exception e) {
			log.error("Failed to execute where-used-query", e);
			return Collections.emptyList();
		}
	}

	private void renderLinks(String label, List<BaseDescriptor> descriptors,
			Image image) {
		if (descriptors.isEmpty())
			return;
		UI.formLabel(parent, toolkit, label);
		Composite linkComposite = toolkit.createComposite(parent);
		UI.gridLayout(linkComposite, 1).verticalSpacing = 0;
		for (BaseDescriptor d : descriptors) {
			ImageHyperlink link = new ImageHyperlink(linkComposite, SWT.TOP);
			link.setText(d.getDisplayName());
			link.setToolTipText(d.getDisplayInfoText());
			link.setImage(image);
			link.addHyperlinkListener(new LinkListener(d));
			link.setForeground(Colors.getLinkBlue());
		}
	}

	private class LinkListener extends HyperlinkAdapter {

		private BaseDescriptor descriptor;

		public LinkListener(BaseDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		@Override
		public void linkActivated(HyperlinkEvent evt) {
			try {
				ProcessDao dao = new ProcessDao(database);
				Process p = dao.getForId(descriptor.getId());
				App.openEditor(p);
			} catch (Exception e) {
				log.error("Failed to open via process-link", e);
			}

		}
	}
}
