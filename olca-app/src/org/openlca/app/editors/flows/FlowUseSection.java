package org.openlca.app.editors.flows;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.openlca.app.editors.UsageView;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the section with links to providers and recipients of a given flow.
 */
class FlowUseSection {

	/**
	 * Shows the maximum number of links that should be shown in a section. A
	 * section is currently not usable or even crashes if there is a huge number
	 * of links.
	 */
	private int MAX_LINKS = 25;

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
		FlowDao dao = new FlowDao(database);
		Set<Long> recipients = dao.getRecipients(flow.getId());
		Set<Long> providers = dao.getProviders(flow.getId());
		if (recipients.isEmpty() && providers.isEmpty())
			return;
		Section section = UI.section(body, toolkit, Messages.UsedInProcesses);
		section.setExpanded(false);
		parent = UI.sectionClient(section, toolkit);
		this.toolkit = toolkit;
		App.runInUI("Render usage links", () -> {
			renderLinks(Messages.ConsumedBy, recipients,
					ImageType.INPUT_ICON.get());
			renderLinks(Messages.ProducedBy, providers,
					ImageType.OUTPUT_ICON.get());
		});
	}

	private void renderLinks(String label, Set<Long> processIds, Image image) {
		if (processIds.isEmpty())
			return;
		UI.formLabel(parent, toolkit, label);
		Composite composite = toolkit.createComposite(parent);
		UI.gridLayout(composite, 1).verticalSpacing = 0;
		for (ProcessDescriptor d : loadDescriptors(processIds)) {
			renderFlowLink(image, composite, d);
		}
		int size = processIds.size();
		if (size > MAX_LINKS) {
			int rest = size - MAX_LINKS;
			renderUsageLink(image, composite, rest);
		}
	}

	private List<ProcessDescriptor> loadDescriptors(Set<Long> ids) {
		if (ids.isEmpty())
			return Collections.emptyList();
		ProcessDao dao = new ProcessDao(database);
		TreeSet<Long> firstIds = new TreeSet<>();
		int i = 0;
		for (Long id : ids) {
			firstIds.add(id);
			i++;
			if (i > MAX_LINKS)
				break;
		}
		List<ProcessDescriptor> list = dao.getDescriptors(firstIds);
		list.sort((d1, d2) -> {
			return Strings.compare(d1.getName(), d2.getName());
		});
		return list;
	}

	private void renderFlowLink(Image image, Composite composite,
			ProcessDescriptor d) {
		ImageHyperlink link = new ImageHyperlink(composite, SWT.TOP);
		link.setText(Labels.getDisplayName(d));
		link.setToolTipText(Labels.getDisplayInfoText(d));
		link.setImage(image);
		link.setForeground(Colors.getLinkBlue());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent evt) {
				ProcessDao dao = new ProcessDao(database);
				Process p = dao.getForId(d.getId());
				App.openEditor(p);
			}
		});
	}

	private void renderUsageLink(Image image, Composite composite, int rest) {
		if (rest < 1)
			return;
		ImageHyperlink link = new ImageHyperlink(composite, SWT.TOP);
		link.setText(rest + " more"); // TODO: @translate
		link.setImage(image);
		link.setForeground(Colors.getLinkBlue());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent evt) {
				UsageView.open(Descriptors.toDescriptor(flow));
			}
		});
	}

}
