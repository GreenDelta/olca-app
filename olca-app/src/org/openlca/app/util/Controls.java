package org.openlca.app.util;

import java.util.function.Consumer;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;

public class Controls {

	private Controls() {
	}

	public static void onSelect(Combo combo, Consumer<SelectionEvent> consumer) {
		combo.addSelectionListener(createSelectionListener(consumer));
	}

	public static void onSelect(Button button, Consumer<SelectionEvent> consumer) {
		button.addSelectionListener(createSelectionListener(consumer));
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
