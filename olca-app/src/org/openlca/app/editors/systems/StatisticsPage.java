package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.systems.Statistics.LinkValue;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

class StatisticsPage extends ModelPage<ProductSystem> {

	private List<Runnable> updates = new ArrayList<>();
	private Statistics stats;
	private ScrolledForm form;
	private TableViewer inLinkTable;
	private TableViewer outLinkTable;

	StatisticsPage(ProductSystemEditor editor) {
		super(editor, "StatisticsPage2", M.Statistics);
	}

	private <T> void bind(T comp, Consumer<T> fn) {
		updates.add(() -> {
			fn.accept(comp);
		});
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		generalSection(tk, body);
		linkDegreeTable(body, tk, true);
		linkDegreeTable(body, tk, false);
		form.reflow(true);
		calculate();
	}

	private void linkDegreeTable(Composite body, FormToolkit tk, boolean inDegree) {
		String title = inDegree
				? "Processes with highest in-degree (linked inputs)"
				: "Processes with highest out-degree (linked outputs)";
		Composite comp = UI.formSection(body, tk, title);
		UI.gridLayout(comp, 1, 0, 10);
		TableViewer table = Tables.createViewer(
				comp, "Processes", "Number of linked inputs");
		Tables.bindColumnWidths(table, 0.5, 0.5);
		table.setLabelProvider(new LinkLabel(() -> {
			if (stats == null)
				return Collections.emptyList();
			return inDegree
					? stats.topInDegrees
					: stats.topOutDegrees;
		}));

		Action onOpen = Actions.onOpen(() -> {
			LinkValue link = Viewers.getFirstSelected(table);
			if (link != null) {
				App.openEditor(link.process);
			}
		});
		Tables.onDoubleClick(table, e -> onOpen.run());
		Actions.bind(table, onOpen);

		if (inDegree) {
			inLinkTable = table;
		} else {
			outLinkTable = table;
		}
	}

	private void generalSection(FormToolkit tk, Composite body) {
		Composite comp = UI.formSection(body, tk, "General statistics");
		UI.gridLayout(comp, 2, 15, 10);
		UI.formLabel(comp, "Number of processes");
		bind(UI.formLabel(comp, ""), label -> {
			label.setText(Integer.toString(stats.processCount));
		});

		UI.formLabel(comp, "Number of process links");
		bind(UI.formLabel(comp, ""), label -> {
			label.setText(Integer.toString(stats.linkCount));
		});

		UI.formLabel(comp, "Connected graph / can calculate?");
		bind(UI.formLabel(comp, ""), label -> {
			String text = stats.connectedGraph
					? "yes"
					: "no";
			label.setText(text);
		});

		UI.formLabel(comp, "Reference process");
		ImageHyperlink link = new ImageHyperlink(comp, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		link.setImage(Images.get(ModelType.PROCESS));
		Controls.onClick(link, e -> {
			if (stats != null) {
				App.openEditor(stats.refProcess);
			}
		});
		bind(link, l -> {
			l.setText(Labels.getDisplayName(stats.refProcess));
		});

		UI.formLabel(comp, "");
		Button btn = tk.createButton(comp, "Recalculate", SWT.NONE);
		Controls.onSelect(btn, e -> calculate());
	}

	private void calculate() {
		App.runWithProgress("Calculate statistics ...", () -> {
			stats = Statistics.calculate(
					getModel(), Cache.getEntityCache());
		}, () -> {
			if (stats != null) {
				for (Runnable update : updates) {
					update.run();
				}
				inLinkTable.setInput(stats.topInDegrees);
				outLinkTable.setInput(stats.topOutDegrees);
				form.reflow(true);
			}
		});
	}

	private class LinkLabel extends LabelProvider implements ITableLabelProvider {

		private Supplier<List<LinkValue>> links;
		private ContributionImage img = new ContributionImage(
				Colors.fromHex("#607d8b"));

		LinkLabel(Supplier<List<LinkValue>> links) {
			this.links = links;
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(ModelType.PROCESS);
			if (col != 1 || !(obj instanceof LinkValue))
				return null;
			int maxDegree = links.get().stream()
					.mapToInt(link -> link.degree).max()
					.orElse(0);
			double share = (double) ((LinkValue) obj).degree
					/ (double) maxDegree;
			return img.getForTable(share);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof LinkValue))
				return null;
			LinkValue link = (LinkValue) obj;
			switch (col) {
			case 0:
				return Labels.getDisplayName(link.process);
			case 1:
				return Integer.toString(link.degree);
			default:
				return null;
			}
		}
	}

}
