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
import org.openlca.app.editors.systems.Statistics.LinkDegree;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

class StatisticsPage extends ModelPage<ProductSystem> {

	private final List<Runnable> updates = new ArrayList<>();
	private Statistics stats;
	private ScrolledForm form;
	private TableViewer inLinkTable;
	private TableViewer outLinkTable;

	StatisticsPage(ProductSystemEditor editor) {
		super(editor, "StatisticsPage2", M.Statistics);
	}

	private <T> void bind(T comp, Consumer<T> fn) {
		updates.add(() -> fn.accept(comp));
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		this.form = UI.header(this);
		var tk = form.getToolkit();
		var body = UI.body(this.form, tk);
		generalSection(tk, body);
		providerSection(tk, body);
		linkDegreeTable(body, tk, true);
		linkDegreeTable(body, tk, false);
		this.form.reflow(true);
		calculate();
	}

	private void generalSection(FormToolkit tk, Composite body) {
		Composite comp = UI.formSection(body, tk, M.GeneralStatistics);
		UI.gridLayout(comp, 2, 15, 10);

		UI.label(comp, tk, M.NumberOfProcesses);
		bind(UI.label(comp, tk, ""),
			label -> label.setText(Integer.toString(stats.processCount)));

		UI.label(comp, tk, M.NumberOfProcessLinks);
		bind(UI.label(comp, tk, ""),
			label -> label.setText(Integer.toString(stats.linkCount)));

		UI.label(comp, tk, M.ConnectGraphCanCalculate);
		bind(UI.label(comp, tk, ""), label -> {
			String text = stats.connectedGraph
				? M.Yes
				: M.No;
			label.setText(text);
		});

		UI.label(comp, tk, M.ReferenceProcess);
		ImageHyperlink link = UI.imageHyperlink(comp, tk, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		link.setImage(Images.get(ModelType.PROCESS));
		Controls.onClick(link, e -> {
			if (stats != null) {
				App.open(stats.refProcess);
			}
		});
		bind(link, l -> l.setText(Labels.name(stats.refProcess)));

		UI.label(comp, tk, "");
		Button btn = UI.button(comp, tk, M.Update);
		Controls.onSelect(btn, e -> calculate());
	}

	private void providerSection(FormToolkit tk, Composite body) {
		Composite comp = UI.formSection(body, tk, M.ProviderLinking);
		UI.gridLayout(comp, 2, 15, 10);

		UI.label(comp, tk, M.LinksToTheDefaultProviders);
		bind(UI.label(comp, tk, ""),
			label -> label.setText(Integer.toString(stats.defaultProviderLinkCount)));

		UI.label(comp, tk, M.LinksWithOnePossibleProvider);
		bind(UI.label(comp, tk, ""),
			label -> label.setText(Integer.toString(stats.singleProviderLinkCount)));

		UI.label(comp, tk, M.LinksWithMultiplePossibleProviders);
		bind(UI.label(comp, tk, ""),
			label -> label.setText(Integer.toString(stats.multiProviderLinkCount)));
	}

	private void linkDegreeTable(Composite body, FormToolkit tk, boolean inDegree) {
		String title = inDegree
			? M.ProcessesWithHighestInDegree
			: M.ProcessesWithHighestOutDegree;
		Composite comp = UI.formSection(body, tk, title);
		UI.gridLayout(comp, 1, 0, 10);
		TableViewer table = Tables.createViewer(
			comp, M.Processes, M.NumberOfLinkedInputs);
		Tables.bindColumnWidths(table, 0.5, 0.5);
		table.setLabelProvider(new LinkDegreeLabel(() -> {
			if (stats == null)
				return Collections.emptyList();
			return inDegree
				? stats.topInDegrees
				: stats.topOutDegrees;
		}));

		Action onOpen = Actions.onOpen(() -> {
			LinkDegree link = Viewers.getFirstSelected(table);
			if (link != null && link.process() != null) {
				App.open(link.process());
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

	private void calculate() {
		App.runWithProgress("Updating statistics ...",
			() -> stats = Statistics.calculate(getModel(), Cache.getEntityCache()),
			() -> {
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

	private static class LinkDegreeLabel extends LabelProvider
		implements ITableLabelProvider {

		private final Supplier<List<LinkDegree>> links;
		private final ContributionImage img = new ContributionImage()
			.withColor(Colors.fromHex("#607d8b"));

		LinkDegreeLabel(Supplier<List<LinkDegree>> links) {
			this.links = links;
		}

		@Override
		public void dispose() {
			img.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof LinkDegree link))
				return null;
			if (col == 0)
				return Images.get(link.process());
			if (col != 1)
				return null;
			int maxDegree = links.get().stream()
				.mapToInt(LinkDegree::degree)
				.max()
				.orElse(0);
			double share = (double) link.degree() / (double) maxDegree;
			return img.get(share);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof LinkDegree link))
				return null;
			return switch (col) {
				case 0 -> Labels.name(link.process());
				case 1 -> Integer.toString(link.degree());
				default -> null;
			};
		}
	}

}
