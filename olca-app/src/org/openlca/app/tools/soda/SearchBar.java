package org.openlca.app.tools.soda;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.ilcd.descriptors.Descriptor;
import org.openlca.ilcd.io.SodaClient;
import org.openlca.ilcd.io.SodaQuery;
import org.openlca.util.Strings;

class SearchBar {

	private final SodaClient client;
	private final TypeCombo typeCombo;
	private final Text searchText;
	private final Spinner pageSpin;
	private final Spinner sizeSpin;
	private Consumer<List<Descriptor<?>>> consumer;

	private SearchBar(Connection con, Composite parent, FormToolkit tk) {
		this.client = con.client();

		var comp = tk.createComposite(parent);
		UI.fillHorizontal(comp);
		var grid = UI.gridLayout(comp, 8);
		grid.marginWidth = 0;
		grid.marginHeight = 0;

		typeCombo = TypeCombo.create(comp, tk, con.hasEpds());
		searchText = tk.createText(comp, "", SWT.BORDER);
		UI.fillHorizontal(searchText);
		searchText.setMessage("Search dataset ...");

		UI.label(comp, tk, "Page: ");
		pageSpin = new Spinner(comp, SWT.BORDER);
		pageSpin.setValues(1, 1, 100, 0, 1, 10);
		UI.label(comp, tk, "Page size: ");
		sizeSpin = new Spinner(comp, SWT.BORDER);
		sizeSpin.setValues(50, 50, 500, 0, 50, 50);

		var button = tk.createButton(comp, "Search", SWT.NONE);
		button.setImage(Icon.SEARCH.get());
		Controls.onSelect(button,	e -> runSearch());
		searchText.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				runSearch();
			}
		});
	}

	static SearchBar create(Connection con, Composite parent, FormToolkit tk) {
		return new SearchBar(con, parent, tk);
	}

	void onResults(Consumer<List<Descriptor<?>>> fn) {
		this.consumer = fn;
	}

	private void runSearch() {
		var type = typeCombo.selected();
		var clazz = Util.classOf(type);
		if (clazz == null)
			return;
		var name = Strings.nullIfEmpty(searchText.getText());
		var page = pageSpin.getSelection() - 1;
		var size = sizeSpin.getSelection();
		var q = new SodaQuery()
				.withStartIndex( page * size)
				.withPageSize(size);
		if (name != null) {
			q.withSearch(true)
					.withName(name);
		}

		var err = new String[1];
		var result = new ArrayList<Descriptor<?>>();
		App.runWithProgress("Search datasets ...", () -> {
			try {
				var list = client.query(clazz, q);
				result.addAll(list.getDescriptors());
			} catch (Exception e) {
				err[0] = e.getMessage();
			}
		}, () -> {
			if (err[0] != null) {
				MsgBox.error("Searching for datasets failed", err[0]);
				return;
			}
			if (consumer != null) {
				consumer.accept(result);
			}
		});
	}

}
