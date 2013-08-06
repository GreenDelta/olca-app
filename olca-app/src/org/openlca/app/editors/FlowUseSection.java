package org.openlca.app.editors;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
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

	FlowUseSection(Flow flow, IDatabase database) {
		this.flow = flow;
		this.database = database;
	}

	void render(Composite body, FormToolkit toolkit) {
		log.trace("render flow-use-section for flow {}", flow);
		List<ProcessDescriptor> recipients = getProcesses(true);
		List<ProcessDescriptor> providers = getProcesses(false);
		if (recipients.isEmpty() && providers.isEmpty())
			return;
		Section section = UI.section(body, toolkit, Messages.UsedInProcesses);
		section.setExpanded(false);
		parent = UI.sectionClient(section, toolkit);
		this.toolkit = toolkit;
		renderLinks(Messages.ConsumedBy, recipients, ImageType.INPUT_ICON.get());
		renderLinks(Messages.ProducedBy, providers, ImageType.OUTPUT_ICON.get());
	}

	private List<ProcessDescriptor> getProcesses(boolean forInput) {
		FlowDao dao = new FlowDao(database);
		if (forInput)
			return dao.getRecipients(flow);
		return dao.getProviders(flow);
	}

	private void renderLinks(String label, List<ProcessDescriptor> descriptors,
			Image image) {
		if (descriptors.isEmpty())
			return;
		UI.formLabel(parent, toolkit, label);
		Composite linkComposite = toolkit.createComposite(parent);
		UI.gridLayout(linkComposite, 1).verticalSpacing = 0;
		for (ProcessDescriptor d : descriptors) {
			ImageHyperlink link = new ImageHyperlink(linkComposite, SWT.TOP);
			link.setText(Labels.getDisplayName(d));
			link.setToolTipText(Labels.getDisplayInfoText(d));
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
			ProcessDao dao = new ProcessDao(database);
			Process p = dao.getForId(descriptor.getId());
			App.openEditor(p);
		}
	}
}
