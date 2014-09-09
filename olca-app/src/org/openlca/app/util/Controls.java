package org.openlca.app.util;

import java.util.function.Consumer;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;

public class Controls {

	private Controls() {
	}

	public static void onSelect(Combo combo, Consumer<SelectionEvent> consumer) {
		combo.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(Button button, Consumer<SelectionEvent> consumer) {
		button.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(MenuItem item, Consumer<SelectionEvent> consumer) {
		item.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(Scale scale, Consumer<SelectionEvent> consumer) {
		scale.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(Link link, Consumer<SelectionEvent> consumer) {
		link.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(Spinner spinner,
			Consumer<SelectionEvent> consumer) {
		spinner.addSelectionListener(createSelectionListener(consumer));
	}

	private static SelectionListener createSelectionListener(
			Consumer<SelectionEvent> consumer) {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				consumer.accept(e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				consumer.accept(e);
			}
		};
	}

}
