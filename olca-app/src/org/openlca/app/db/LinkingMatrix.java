package org.openlca.app.db;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

class LinkingMatrix {

	private final boolean multiProviders;
	private final boolean missingProviders;

	private final Color black = Colors.black();
	private final Color white = Colors.white();

	private final Color darkGrey = Colors.fromHex("#b0bec5");
	private final Color darkRed = Colors.fromHex("#e91e63");
	private final Color darkGreen = Colors.fromHex("#4caf50");
	private final Color darkOrange = Colors.fromHex("#ff9800");

	private final Color lightGrey = Colors.fromHex("#eceff1");
	private final Color lightRed = Colors.fromHex("#fce4ec");
	private final Color lightGreen = Colors.fromHex("#e8f5e9");
	private final Color lightOrange = Colors.fromHex("#fff3e0");

	LinkingMatrix(LinkingProperties props) {
		if (props != null) {
			multiProviders = !props.multiProviderFlows.isEmpty();
			missingProviders = !props.processesWithoutProviders.isEmpty();
		} else {
			multiProviders = false;
			missingProviders = false;
		}
	}

	void render(Composite root, FormToolkit tk) {
		Composite body = tk.createComposite(root);
		UI.gridData(body, false, false).widthHint = 800;

		body.setBackground(Colors.white());

		GridLayout grid = new GridLayout(6, true);
		grid.marginWidth = 10;
		body.setLayout(grid);

		headerCell(body, tk, "Linking properties");
		headerCell(body, tk, "Product system creation: Linking option");

		filler(body);
		filler(body);
		cell(body, "Product flows with multiple providers");
		cell(body, "Ignore default providers", black, darkGrey);
		cell(body, "Prefer default providers", black, darkGrey);
		cell(body, "Only default providers", black, darkGrey);

		cell(body, "Processes without default providers", 4);
		cell(body, "Yes", 2, black, missingProviders ? darkGrey : lightGrey);
		if (missingProviders & multiProviders) {
			cell(body, "Yes", black, darkGrey);
			cell(body, "ambiguous", white, darkRed);
			cell(body, "ambiguous", white, darkRed);
			cell(body, "incomplete", white, darkOrange);
		} else {
			cell(body, "Yes", black, lightGrey);
			cell(body, "", black, lightRed);
			cell(body, "", black, lightRed);
			cell(body, "", black, lightOrange);
		}

		if (missingProviders && !multiProviders) {
			cell(body, "No", black, darkGrey);
			cell(body, "ok", white, darkGreen);
			cell(body, "ok", white, darkGreen);
			cell(body, "incomplete", white, darkOrange);
		} else {
			cell(body, "No", black, lightGrey);
			cell(body, "", black, lightGreen);
			cell(body, "", black, lightGreen);
			cell(body, "", black, lightOrange);
		}

		cell(body, "No", 2, black, missingProviders ? lightGrey : darkGrey);
		if (!missingProviders && multiProviders) {
			cell(body, "Yes", black, darkGrey);
			cell(body, "ambiguous", white, darkRed);
			cell(body, "ok", white, darkGreen);
			cell(body, "ok", white, darkGreen);
		} else {
			cell(body, "Yes", black, lightGrey);
			cell(body, "", black, lightRed);
			cell(body, "", black, lightGreen);
			cell(body, "", black, lightGreen);
		}

		if (!missingProviders && !multiProviders) {
			cell(body, "No", black, darkGrey);
			cell(body, "ok", white, darkGreen);
			cell(body, "ok", white, darkGreen);
			cell(body, "ok", white, darkGreen);
		} else {
			cell(body, "No", black, lightGrey);
			cell(body, "", black, lightGreen);
			cell(body, "", black, lightGreen);
			cell(body, "", black, lightGreen);
		}
	}

	private void headerCell(Composite body, FormToolkit tk, String text) {
		Composite c = tk.createComposite(body, SWT.BORDER);
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		c.setLayout(layout);
		UI.gridData(c, true, false).horizontalSpan = 3;
		Label label = tk.createLabel(c, text, SWT.CENTER);
		label.setFont(UI.boldFont());
	}

	private Label filler(Composite parent) {
		Label f = new Label(parent, SWT.NONE);
		f.setBackground(Colors.white());
		return f;
	}

	private Label cell(Composite parent, String text) {
		return cell(parent, text, 1, black, lightGrey);
	}

	private Label cell(Composite parent, String text,
			Color foreground, Color background) {
		return cell(parent, text, 1, foreground, background);
	}

	private Label cell(Composite parent, String text,
			int vspan) {
		return cell(parent, text, vspan, black, lightGrey);
	}

	private Label cell(
			Composite parent,
			String text,
			int vspan,
			Color foreground,
			Color background) {
		Composite comp = new Composite(parent, SWT.BORDER);
		comp.setLayoutData(new GridData(
				SWT.FILL, SWT.FILL, true, false, 1, vspan));
		comp.setBackground(background);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		comp.setLayout(layout);
		Label label = new Label(comp, SWT.WRAP | SWT.CENTER);
		label.setForeground(foreground);
		label.setBackground(background);
		if (foreground == white) {
			label.setFont(UI.boldFont());
		}
		GridData data = new GridData(
				SWT.FILL, SWT.CENTER, true, false, 1, 1);
		label.setLayoutData(data);
		if (text != null) {
			label.setText(text);
		}
		return label;
	}

}
