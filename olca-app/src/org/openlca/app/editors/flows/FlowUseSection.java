package org.openlca.app.editors.flows;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/// Renders the section with links to providers and recipients of a given flow.
class FlowUseSection {


	/// The maximum number of links that should be shown in a section. A section is
	/// currently not usable or even crashes if there is a large number of links.
	private final int MAX_LINKS = 10;

	private final Flow flow;
	private final IDatabase db;
	private Composite parent;
	private FormToolkit tk;

	FlowUseSection(Flow flow, IDatabase db) {
		this.flow = flow;
		this.db = db;
	}

	void render(Composite body, FormToolkit tk) {
		var dao = new FlowDao(db);
		Set<Long> recipients = dao.getWhereInput(flow.id);
		Set<Long> providers = dao.getWhereOutput(flow.id);
		if (recipients.isEmpty() && providers.isEmpty())
			return;
		var section = UI.section(body, tk, M.UsedInProcesses);
		section.setExpanded(false);
		parent = UI.sectionClient(section, tk);
		this.tk = tk;
		App.runInUI(M.RenderUsageLinks, () -> {
			renderLinks(M.ConsumedBy, recipients, Icon.INPUT.get());
			renderLinks(M.ProducedBy, providers, Icon.OUTPUT.get());
		});
	}

	private void renderLinks(String label, Set<Long> processIds, Image image) {
		if (processIds.isEmpty())
			return;
		UI.label(parent, tk, label);
		var composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 1).verticalSpacing = 0;
		for (var d : loadDescriptors(processIds)) {
			renderFlowLink(image, composite, d);
		}
		int size = processIds.size();
		if (size > MAX_LINKS) {
			int rest = size - MAX_LINKS;
			renderUsageLink(image, composite, rest, label, processIds);
		}
	}

	private List<ProcessDescriptor> loadDescriptors(Set<Long> ids) {
		if (ids.isEmpty())
			return Collections.emptyList();
		var dao = new ProcessDao(db);
		var firstIds = new TreeSet<Long>();
		int i = 0;
		for (var id : ids) {
			firstIds.add(id);
			i++;
			if (i > MAX_LINKS)
				break;
		}
		var list = dao.getDescriptors(firstIds);
		list.sort(
			(di, dj) -> Strings.compareIgnoreCase(Labels.name(di), Labels.name(dj)));
		return list;
	}

	private void renderFlowLink(Image image, Composite comp, ProcessDescriptor d) {
		var link = UI.imageHyperlink(comp, tk, SWT.TOP);
		link.setText(Labels.name(d));
		link.setImage(image);
		link.setForeground(Colors.linkBlue());
		Controls.onClick(link, e -> {
			ProcessDao dao = new ProcessDao(db);
			Process p = dao.getForId(d.id);
			App.open(p);
		});
	}

	private void renderUsageLink(
			Image image,
			Composite comp,
			int rest,
			String usageType,
			Set<Long> processIds
	) {
		if (rest < 1)
			return;
		var link = UI.imageHyperlink(comp, tk, SWT.TOP);
		link.setText(M.More + " (" + rest + ")");
		link.setImage(image);
		link.setForeground(Colors.linkBlue());
		Controls.onClick(link,
				e -> FlowUsePopup.show(flow, processIds, usageType));
	}
}
