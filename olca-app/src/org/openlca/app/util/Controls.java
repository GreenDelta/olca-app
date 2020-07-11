package org.openlca.app.util;

import java.util.function.Consumer;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class Controls {

	private Controls() {
	}

	public static void set(Text text, String s) {
		if (text == null)
			return;
		if (s == null) {
			text.setText("");
		} else {
			text.setText(s);
		}
	}

	public static void onSelect(Combo combo, Consumer<SelectionEvent> consumer) {
		combo.addSelectionListener(handle(consumer));
	}

	public static void onSelect(Button button, Consumer<SelectionEvent> consumer) {
		button.addSelectionListener(handle(consumer));
	}

	public static void onSelect(MenuItem item, Consumer<SelectionEvent> consumer) {
		item.addSelectionListener(handle(consumer));
	}

	public static void onSelect(Scale scale, Consumer<SelectionEvent> consumer) {
		scale.addSelectionListener(handle(consumer));
	}

	public static void onSelect(Link link, Consumer<SelectionEvent> consumer) {
		link.addSelectionListener(handle(consumer));
	}

	public static void onSelect(Spinner spinner, Consumer<SelectionEvent> consumer) {
		spinner.addSelectionListener(handle(consumer));
	}

	public static void onSelect(TableCombo combo, Consumer<SelectionEvent> fn) {
		combo.addSelectionListener(handle(fn));
	}

	public static void onSelect(List list, Consumer<SelectionEvent> fn) {
		list.addSelectionListener(handle(fn));
	}

	private static SelectionListener handle(Consumer<SelectionEvent> fn) {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fn.accept(e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				fn.accept(e);
			}
		};
	}

	public static void onClick(Hyperlink link, Consumer<HyperlinkEvent> fn) {
		if (link == null || fn == null)
			return;
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				fn.accept(e);
			}
		});
	}
}
