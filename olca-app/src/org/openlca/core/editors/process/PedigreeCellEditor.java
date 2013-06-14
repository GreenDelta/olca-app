package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.core.model.Exchange;

class PedigreeCellEditor extends CellEditor {

	private Exchange exchange;

	public PedigreeCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected Control createControl(final Composite parent) {
		Hyperlink hyperlink = new Hyperlink(parent, SWT.NONE);
		hyperlink.setText("Click to change");
		hyperlink.setBackground(parent.getShell().getDisplay()
				.getSystemColor(SWT.COLOR_WHITE));
		hyperlink.setForeground(parent.getShell().getDisplay()
				.getSystemColor(SWT.COLOR_BLUE));
		hyperlink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				PedigreeShell shell = new PedigreeShell(parent.getShell(),
						exchange);
				shell.open();
			}

		});
		return hyperlink;
	}

	@Override
	protected Object doGetValue() {
		return exchange;
	}

	@Override
	protected void doSetFocus() {
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof Exchange)
			exchange = (Exchange) value;
		else
			exchange = null;
	}

}
