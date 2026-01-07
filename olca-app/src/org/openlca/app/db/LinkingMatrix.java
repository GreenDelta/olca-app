package org.openlca.app.db;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
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
		Composite body = UI.composite(root, tk);
		UI.gridData(body, false, false).widthHint = 800;

		GridLayout grid = new GridLayout(6, true);
		grid.marginWidth = 10;
		body.setLayout(grid);

		headerCell(body, tk, M.LinkingProperties);
		headerCell(body, tk, M.ProductSystemCreationLinkingOption);

		filler(body);
		filler(body);
		cell(body, M.ProductFlowWithMultipleProviders, black, lightGrey);
		cell(body, M.IgnoreDefaultProviders, getForegroundForBackground(darkGrey), darkGrey);
		cell(body, M.PreferDefaultProviders, getForegroundForBackground(darkGrey), darkGrey);
		cell(body, M.OnlyDefaultProviders, getForegroundForBackground(darkGrey), darkGrey);

		cell(body, M.ProcessesWithoutDefaultProvider, 4, black, lightGrey);
		Color yesBg = missingProviders ? darkGrey : lightGrey;
		cell(body, M.Yes, 2, getForegroundForBackground(yesBg), yesBg);
		if (missingProviders & multiProviders) {
			cell(body, M.Yes, getForegroundForBackground(darkGrey), darkGrey);
			cell(body, M.Ambiguous, getForegroundForBackground(darkRed), darkRed);
			cell(body, M.Ambiguous, getForegroundForBackground(darkRed), darkRed);
			cell(body, M.Incomplete, getForegroundForBackground(darkOrange), darkOrange);
		} else {
			cell(body, M.Yes, getForegroundForBackground(lightGrey), lightGrey);
			cell(body, "", getForegroundForBackground(lightRed), lightRed);
			cell(body, "", getForegroundForBackground(lightRed), lightRed);
			cell(body, "", getForegroundForBackground(lightOrange), lightOrange);
		}

		if (missingProviders && !multiProviders) {
			cell(body, M.No, getForegroundForBackground(darkGrey), darkGrey);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
			cell(body, M.Incomplete, getForegroundForBackground(darkOrange), darkOrange);
		} else {
			cell(body, M.No, getForegroundForBackground(lightGrey), lightGrey);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
			cell(body, "", getForegroundForBackground(lightOrange), lightOrange);
		}

		Color noBg = missingProviders ? lightGrey : darkGrey;
		cell(body, M.No, 2, getForegroundForBackground(noBg), noBg);
		if (!missingProviders && multiProviders) {
			cell(body, M.Yes, getForegroundForBackground(darkGrey), darkGrey);
			cell(body, M.Ambiguous, getForegroundForBackground(darkRed), darkRed);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
		} else {
			cell(body, M.Yes, getForegroundForBackground(lightGrey), lightGrey);
			cell(body, "", getForegroundForBackground(lightRed), lightRed);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
		}

		if (!missingProviders && !multiProviders) {
			cell(body, M.No, getForegroundForBackground(darkGrey), darkGrey);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
			cell(body, M.OK, getForegroundForBackground(darkGreen), darkGreen);
		} else {
			cell(body, M.No, getForegroundForBackground(lightGrey), lightGrey);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
			cell(body, "", getForegroundForBackground(lightGreen), lightGreen);
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
		return UI.label(parent);
	}

	/**
	 * Determines the appropriate foreground color based on the background color.
	 * - Grey backgrounds (darkGrey, lightGrey) -> black font
	 * - Dark red/green/orange backgrounds -> white font
	 * - Light backgrounds -> black font
	 */
	private Color getForegroundForBackground(Color background) {
		if (background == darkRed || background == darkGreen || background == darkOrange) {
			return white;
		}
		// For grey backgrounds and light backgrounds, use black
		return black;
	}

	private Label cell(Composite parent, String text,
			Color foreground, Color background) {
		return cell(parent, text, 1, foreground, background);
	}

	private Label cell(
			Composite parent,
			String text,
			int vspan,
			Color foreground,
			Color background) {
		Composite comp = UI.composite(parent, SWT.BORDER);
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
		
		// With the app in dark mode, it is necessary to re-set the background color
		// when painting. Otherwise, the background stays dark due to CSS overrides.
		PaintListener paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (!comp.isDisposed() && !label.isDisposed()) {
					label.setBackground(background);
					comp.setBackground(background);
					label.setForeground(foreground);
				}
			}
		};
		comp.addPaintListener(paintListener);
		label.addPaintListener(paintListener);
		
		return label;
	}

}
