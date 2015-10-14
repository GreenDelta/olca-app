package org.openlca.app.cloud.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;

import com.google.gson.JsonObject;

public class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private StyledText localText;
	private StyledText remoteText;
	private final Color FIELD_COLOR;

	public DiffEditor(Composite parent) {
		this(parent, null);
	}

	public DiffEditor(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		FIELD_COLOR = new Color(Display.getCurrent(), 131, 5, 131);
		this.toolkit = toolkit;
		initialize();
		if (toolkit != null)
			toolkit.adapt(this);
	}

	private void initialize() {
		GridLayout layout = UI.gridLayout(this, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		localText = createTextPart("#Local model");
		remoteText = createTextPart("#Remote model");
	}

	private StyledText createTextPart(String label) {
		Composite localComposite = UI.formComposite(this, toolkit);
		UI.gridLayout(localComposite, 1, 0, 0);
		UI.gridData(localComposite, true, true);
		UI.formLabel(localComposite, toolkit, label);
		StyledText text = new StyledText(localComposite, SWT.BORDER | SWT.MULTI
				| SWT.H_SCROLL | SWT.V_SCROLL);
		if (toolkit != null)
			toolkit.adapt(text);
		UI.gridData(text, true, true);
		text.addListener(SWT.Resize, this::onResize);
		text.addListener(SWT.Modify, this::onResize);
		return text;
	}

	public void setInput(JsonObject local, JsonObject remote) {
		localText.setText(JsonToText.toText(local));
		style(localText);
		remoteText.setText(JsonToText.toText(remote));
		style(remoteText);
	}

	private void style(StyledText widget) {
		String text = widget.getText();
		String[] lines = text.split("\n");
		int cursor = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (!line.contains(":"))
				continue;
			int leadingSpaces = countLeadingSpaces(line);
			int start = cursor + leadingSpaces;
			int length = line.indexOf(':') - leadingSpaces;
			widget.setStyleRange(range(FIELD_COLOR, start, length, true));
			cursor += line.length() + 1; // +1 for \n
		}
	}

	private int countLeadingSpaces(String value) {
		for (int i = 0; i < value.length(); i++)
			if (value.charAt(i) != ' ')
				return i;
		return 0;
	}

	private StyleRange range(Color color, int start, int length, boolean bold) {
		StyleRange range = new StyleRange();
		range.start = start;
		range.length = length;
		if (bold)
			range.fontStyle = SWT.BOLD;
		range.foreground = color;
		return range;
	}

	private void onResize(Event event) {
		StyledText text = (StyledText) event.widget;
		Rectangle r1 = text.getClientArea();
		Rectangle r2 = text.computeTrim(r1.x, r1.y, r1.width, r1.height);
		Point p = text.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		text.getHorizontalBar().setVisible(r2.width <= p.x);
		text.getVerticalBar().setVisible(r2.height <= p.y);
		if (event.type == SWT.Modify) {
			text.getParent().layout(true);
			text.showSelection();
		}
	}

}
