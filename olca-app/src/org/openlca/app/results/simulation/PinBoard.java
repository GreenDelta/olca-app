package org.openlca.app.results.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.math.Simulator;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.matrix.TechIndex;
import org.openlca.util.Strings;

/**
 * A section for selecting processes or sub-systems (in general "providers") of
 * which the contribution results (direct and upstream) should be collected via
 * a simulation.
 */
class PinBoard {

	private final Simulator simulator;

	private Text filter;
	private TableViewer table;
	private ProcessProduct resultPin;
	Consumer<ProcessProduct> onResultPinChange;

	PinBoard(Simulator simulator) {
		this.simulator = simulator;
	}

	void create(FormToolkit tk, Composite body) {
		Section section = UI.section(body, tk, "Pinned contributions");
		section.setExpanded(false);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		Composite filterComp = tk.createComposite(comp);
		UI.gridLayout(filterComp, 2, 10, 0);
		UI.gridData(filterComp, true, false);
		filter = UI.formText(filterComp, tk, M.Filter);
		filter.addModifyListener(e -> table.setInput(selectInput()));

		table = Tables.createViewer(comp,
				"Pin / Unpin",
				"Process / Sub-System",
				M.Product,
				"Display in chart");
		Tables.bindColumnWidths(table, 0.15, 0.35, 0.35, 0.15);
		Label label = new Label();
		table.setLabelProvider(label);
		Viewers.sortByLabels(table, label, 0, 1);
		table.setInput(selectInput());
		bindActions();
	}

	private void bindActions() {
		Action open = Actions.onOpen(() -> {
			ProcessProduct pp = Viewers.getFirstSelected(table);
			if (pp == null)
				return;
			App.open(pp.process);
		});
		Tables.onDoubleClick(table, e -> open.run());

		Action pin = Actions.create(
				"Pin / Unpin", Icon.CHECK_TRUE.descriptor(), () -> {
					ProcessProduct pp = Viewers.getFirstSelected(table);
					onPin(pp);
				});

		table.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button != 1)
					return;
				Point p = new Point(e.x, e.y);
				ViewerCell cell = table.getCell(p);
				if (cell == null)
					return;
				Object data = cell.getItem().getData();
				if (!(data instanceof ProcessProduct))
					return;
				ProcessProduct pp = (ProcessProduct) data;
				if (cell.getColumnIndex() == 0) {
					onPin(pp);
				} else if (cell.getColumnIndex() == 3) {
					onResultPin(pp);
				}
			}
		});

		Actions.bind(table, pin, open);
	}

	private void onPin(ProcessProduct pp) {
		if (pp == null)
			return;
		boolean pinned = simulator.pinnedProducts.contains(pp);
		if (pinned) {
			simulator.pinnedProducts.remove(pp);
			if (Objects.equals(pp, resultPin)) {
				resultPin = null;
				if (onResultPinChange != null) {
					onResultPinChange.accept(null);
				}
			}
		} else {
			simulator.pinnedProducts.add(pp);
		}
		table.setInput(selectInput());
	}

	private void onResultPin(ProcessProduct pp) {
		if (pp == null)
			return;
		if (Objects.equals(pp, resultPin)) {
			resultPin = null;
			if (onResultPinChange != null) {
				onResultPinChange.accept(null);
			}
		} else {
			resultPin = pp;
			if (onResultPinChange != null) {
				onResultPinChange.accept(pp);
			}
		}
		table.refresh();
	}

	private List<ProcessProduct> selectInput() {
		// f is a possible text filter
		String f = null;
		if (filter != null) {
			String text = filter.getText();
			if (!Strings.nullOrEmpty(text)) {
				f = text.trim().toLowerCase(Locale.US);
			}
		}

		// apply possible text filter
		List<ProcessProduct> input = new ArrayList<>();
		TechIndex idx = simulator.getResult().techIndex;
		for (int i = 0; i < idx.size(); i++) {
			ProcessProduct pp = idx.at(i);

			// pinned products are never filtered
			if (simulator.pinnedProducts.contains(pp)) {
				input.add(pp);
				continue;
			}

			// no filter
			if (f == null) {
				input.add(pp);
				continue;
			}

			// process name matches
			String s = Labels.name(
					pp.process).toLowerCase(Locale.US);
			if (s.contains(f)) {
				input.add(pp);
				continue;
			}

			// product name matches
			s = Labels.name(
					pp.flow).toLowerCase(Locale.US);
			if (s.contains(f)) {
				input.add(pp);
				continue;
			}
		}

		// sort by provider name
		input.sort((pp1, pp2) -> {
			// pinned products are always sorted to the top
			boolean pinned1 = simulator.pinnedProducts.contains(pp1);
			boolean pinned2 = simulator.pinnedProducts.contains(pp2);
			if (pinned1 && !pinned2)
				return -1;
			if (!pinned1 && pinned2)
				return 1;
			String s1 = Labels.name(pp1.process);
			String s2 = Labels.name(pp2.process);
			return Strings.compare(s1, s2);
		});

		return input;
	}

	private class Label extends LabelProvider
			implements ITableLabelProvider, IFontProvider {

		@Override
		public Font getFont(Object obj) {
			if (!(obj instanceof ProcessProduct))
				return null;
			ProcessProduct pp = (ProcessProduct) obj;
			if (simulator.pinnedProducts.contains(pp))
				return UI.boldFont();
			return null;
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProcessProduct))
				return null;
			ProcessProduct pp = (ProcessProduct) obj;
			switch (col) {
			case 0:
				boolean pinned = simulator.pinnedProducts.contains(pp);
				return pinned
						? Icon.CHECK_TRUE.get()
						: Icon.CHECK_FALSE.get();
			case 1:
				return Images.get(pp.process);
			case 2:
				return Images.get(pp.flow);
			case 3:
				boolean isResult = Objects.equals(pp, resultPin);
				return isResult
						? Icon.CHECK_TRUE.get()
						: Icon.CHECK_FALSE.get();
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProcessProduct))
				return null;
			ProcessProduct pp = (ProcessProduct) obj;
			if (col == 1)
				return Labels.name(pp.process);
			else if (col == 2)
				return Labels.name(pp.flow);
			return null;
		}
	}
}
