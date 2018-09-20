package org.openlca.app.util;

import java.net.CookieHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Images;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Worker.State;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class UI {

	private UI() {
	}

	public static WebEngine createWebView(FXCanvas canvas) {
		canvas.setLayout(new FillLayout());
		WebView view = new WebView();
		// When the WebEngine is initialized a CookieHandler is set, which has
		// errors reading multi value cookies, therefore set to null again
		CookieHandler.setDefault(null);
		Scene scene = new Scene(view);
		canvas.setScene(scene);
		WebEngine webkit = view.getEngine();
		webkit.setJavaScriptEnabled(true);
		webkit.setOnAlert(e -> {
			Logger log = LoggerFactory.getLogger(UI.class);
			log.error("JavaScript alert: {}", e.getData());
		});
		return webkit;
	}

	public static Control createWebView(Composite parent, WebPage page) {
		FXCanvas canvas = new FXCanvas(parent, SWT.NONE);
		WebEngine webkit = UI.createWebView(canvas);
		AtomicBoolean firstCall = new AtomicBoolean(true);
		webkit.getLoadWorker().stateProperty().addListener((v, old, newState) -> {
			if (firstCall.get() && newState == State.SUCCEEDED) {
				firstCall.set(false);
				page.onLoaded(webkit);
			}
		});
		webkit.load(page.getUrl());
		return canvas;
	}

	public static void bindVar(WebEngine webkit, String name, Object var) {
		try {
			JSObject window = (JSObject) webkit.executeScript("window");
			window.setMember(name, var);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(UI.class);
			log.error("failed to bind {} as {}", var, name, e);
		}
	}

	public static Shell shell() {
		Shell shell = null;
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		if (display != null)
			shell = display.getActiveShell();
		if (shell == null)
			if (display != null)
				shell = new Shell(display);
			else
				shell = new Shell();
		return shell;
	}

	public static Font boldFont() {
		return JFaceResources.getFontRegistry().getBold(
				JFaceResources.DEFAULT_FONT);
	}

	public static void center(Shell parent, Shell child) {
		Rectangle shellBounds = parent.getBounds();
		Point size = child.getSize();
		int diffX = (shellBounds.width - size.x) / 2;
		int diffY = (shellBounds.height - size.y) / 2;
		child.setLocation(shellBounds.x + diffX, shellBounds.y + diffY);
	}

	public static GridData gridData(Control control, boolean hFill, boolean vFill) {
		int hStyle = hFill ? SWT.FILL : SWT.LEFT;
		int vStyle = vFill ? SWT.FILL : SWT.CENTER;
		GridData data = new GridData(hStyle, vStyle, hFill, vFill);
		control.setLayoutData(data);
		return data;
	}

	public static ScrolledForm formHeader(ModelPage<?> page) {
		Image image = Images.get(page.getEditor().getModel());
		ScrolledForm form = formHeader(page.getManagedForm(), page.getFormTitle(), image);
		// "" is 'general' comment on data set
		if (page.getEditor().hasComment("")) {
			form.getToolBarManager().add(new CommentAction("", page.getEditor().getComments()));
		}
		Editors.addRefresh(form, page.getEditor());
		form.getToolBarManager().update(true);
		return form;
	}

	/** Creates a nice form header with the given title and returns the form. */
	public static ScrolledForm formHeader(IManagedForm mform, String title) {
		return formHeader(mform, title, null);
	}

	public static ScrolledForm formHeader(IManagedForm mform, String title, Image image) {
		ScrolledForm form = mform.getForm();
		FormToolkit tk = mform.getToolkit();
		tk.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		if (title != null)
			form.setText(title);
		if (image != null)
			form.setImage(image);
		tk.decorateFormHeading(form.getForm());
		return form;
	}

	public static Composite formSection(Composite parent, FormToolkit tk, String label) {
		return formSection(parent, tk, label, 2);
	}

	public static Composite formSection(Composite parent, FormToolkit tk,
			String label, int columns) {
		Section section = section(parent, tk, label);
		Composite client = sectionClient(section, tk, columns);
		return client;
	}

	public static Section section(Composite parent, FormToolkit toolkit, String label) {
		Section section = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		gridData(section, true, false);
		section.setText(label);
		return section;
	}

	/**
	 * Creates a composite and sets it as section client of the given section. The
	 * created composite gets a 2-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit) {
		return sectionClient(section, toolkit, 2);
	}

	/**
	 * Creates a composite and sets it as section client of the given section. The
	 * created composite gets a n-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit, int columns) {
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		gridLayout(composite, columns);
		return composite;
	}

	public static Composite formBody(ScrolledForm form, FormToolkit tk) {
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginRight = 10;
		layout.marginLeft = 10;
		layout.horizontalSpacing = 10;
		layout.marginBottom = 10;
		layout.marginTop = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		body.setLayout(layout);
		tk.paintBordersFor(body);
		gridData(body, true, true);
		return body;
	}

	public static GridLayout gridLayout(Composite composite, int columns) {
		return gridLayout(composite, columns, 10, 10);
	}

	public static GridLayout gridLayout(Composite composite, int columns,
			int spacing, int margin) {
		final GridLayout layout = new GridLayout(columns, false);
		layout.verticalSpacing = spacing;
		layout.marginWidth = margin;
		layout.marginHeight = margin;
		layout.horizontalSpacing = spacing;
		composite.setLayout(layout);
		return layout;
	}

	public static Composite formComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		gridLayout(composite, 2);
		return composite;
	}

	public static Composite formComposite(Composite parent, FormToolkit toolkit) {
		if (toolkit == null)
			return formComposite(parent);
		Composite composite = toolkit.createComposite(parent);
		gridLayout(composite, 2);
		return composite;
	}

	/** Creates a simple check box with the given text. */
	public static Button checkBox(Composite parent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);
		return button;
	}

	/** Creates a label and check box as two separate components. */
	public static Button formCheckBox(Composite parent, String label) {
		return formCheckBox(parent, null, label);
	}

	/** Creates a label and check box as two separate components. */
	public static Button formCheckBox(Composite parent, FormToolkit toolkit, String label) {
		formLabel(parent, toolkit, label);
		return formCheckbox(parent, toolkit);
	}

	public static Button formCheckbox(Composite parent, FormToolkit toolkit) {
		Button button = null;
		if (toolkit != null)
			button = toolkit.createButton(parent, null, SWT.CHECK);
		else
			button = new Button(parent, SWT.CHECK);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		return button;
	}

	public static Button formRadio(Composite parent, String label) {
		Button button = new Button(parent, SWT.RADIO);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		formLabel(parent, label);
		return button;
	}

	public static Text formText(Composite parent, int flags) {
		return formText(parent, null, null, flags | SWT.BORDER);
	}

	public static Text formText(Composite parent, String label) {
		return formText(parent, null, label);
	}

	public static Text formText(Composite parent, String label, int flags) {
		return formText(parent, null, label, flags | SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit toolkit,
			String label) {
		return formText(parent, toolkit, label, SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit toolkit,
			String label, int flags) {
		if (label != null)
			formLabel(parent, toolkit, label);
		Text text = null;
		if (toolkit != null)
			text = toolkit.createText(parent, null, flags);
		else
			text = new Text(parent, flags);
		gridData(text, true, false);
		return text;
	}

	public static Text formMultiText(Composite parent, String label) {
		return formMultiText(parent, null, label);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, String label) {
		return formMultiText(comp, tk, label, 100);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, String label, int heightHint) {
		formLabel(comp, tk, label);
		return formMultiText(comp, tk, heightHint);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk) {
		return formMultiText(comp, tk, 100);
	}

	public static Text formMultiText(Composite comp, FormToolkit tk, int heightHint) {
		Text text = null;
		if (tk != null) {
			text = tk.createText(comp, null, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		} else {
			text = new Text(comp, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		}
		GridData gd = gridData(text, true, false);
		gd.minimumHeight = heightHint;
		gd.heightHint = heightHint;
		gd.widthHint = heightHint;
		return text;
	}

	public static Combo formCombo(Composite comp, String label) {
		return formCombo(comp, null, label);
	}

	public static Combo formCombo(Composite comp, FormToolkit tk, String label) {
		formLabel(comp, tk, label);
		Combo combo = new Combo(comp, SWT.READ_ONLY);
		gridData(combo, true, false);
		return combo;
	}

	public static Label formLabel(Composite comp, String text) {
		return formLabel(comp, null, text);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text) {
		Label label = null;
		if (tk != null) {
			label = tk.createLabel(comp, text, SWT.NONE);
		} else {
			label = new Label(comp, SWT.NONE);
			label.setText(text);
		}
		GridData gd = gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		return label;
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static void filler(Composite comp, FormToolkit tk) {
		formLabel(comp, tk, "");
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static void filler(Composite comp) {
		formLabel(comp, null, "");
	}

	public static Hyperlink formLink(Composite parent, String label) {
		return formLink(parent, null, label);
	}

	public static Hyperlink formLink(Composite comp, FormToolkit tk, String text) {
		Hyperlink link = null;
		if (tk != null)
			link = tk.createHyperlink(comp, text, SWT.NONE);
		else {
			link = new Hyperlink(comp, SWT.NONE);
			link.setText(text);
		}
		GridData gd = gridData(link, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		return link;
	}

}
