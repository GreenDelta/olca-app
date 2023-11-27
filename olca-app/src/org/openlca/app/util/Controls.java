package org.openlca.app.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
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
		text.setText(s == null ? "" : s);
	}

	public static void set(Text text, String initial, Consumer<String> fn) {
		set(text, initial);
		if (text == null || fn == null)
			return;
		text.addModifyListener($ -> fn.accept(text.getText()));
	}

	public static void set(Text text, double initial, DoubleConsumer fn) {
		set(text, Double.toString(initial));
		if (text == null || fn == null)
			return;
		text.addModifyListener($ -> {
			try {
				var d = Double.parseDouble(text.getText());
				fn.accept(d);
				text.setBackground(Colors.white());
			} catch (NumberFormatException e) {
				text.setBackground(Colors.errorColor());
			}
		});
	}

	public static <T extends Control> void onPainted(T widget, Runnable fn) {
		if (widget == null || fn == null)
			return;
		var listener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				fn.run();
				widget.removePaintListener(this);
			}
		};
		widget.addPaintListener(listener);
	}

	public static void onSelect(Combo combo, Consumer<SelectionEvent> consumer) {
		combo.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(Button button, Consumer<SelectionEvent> consumer) {
		button.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(MenuItem item, Consumer<SelectionEvent> consumer) {
		item.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(Scale scale, Consumer<SelectionEvent> consumer) {
		scale.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(Link link, Consumer<SelectionEvent> consumer) {
		link.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(Spinner spinner, Consumer<SelectionEvent> consumer) {
		spinner.addSelectionListener(onSelect(consumer));
	}

	public static void onSelect(TableCombo combo, Consumer<SelectionEvent> fn) {
		combo.addSelectionListener(onSelect(fn));
	}

	public static void onSelect(List list, Consumer<SelectionEvent> fn) {
		list.addSelectionListener(onSelect(fn));
	}

	public static SelectionListener onSelect(Consumer<SelectionEvent> fn) {
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

	public static void onReturn(Text text, Consumer<TraverseEvent> fn) {
		if (text == null || fn == null)
			return;
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				fn.accept(e);
			}
		});
	}

	/**
	 * Checks and, if required, sets the given color as background color on every
	 * paint event. On some systems and especially in dark-mode this is necessary
	 * in some cases because otherwise the widget is drawn with the default
	 * background color even when it was set explicitly. It also does not always
	 * work to do this in the first paint event only, thus we need to check the
	 * background color after each re-paint. So, only use this function if
	 * required.
	 */
	public static void paintBackground(Control widget, Color color) {
		if (widget == null || color == null)
			return;
		widget.setBackground(color);
		widget.addPaintListener(e -> {
			if (Objects.equals(widget.getBackground(), color))
				return;
			widget.setBackground(color);
		});
	}
}
