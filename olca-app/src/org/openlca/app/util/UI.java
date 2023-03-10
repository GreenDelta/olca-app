package org.openlca.app.util;

import java.util.function.Function;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.*;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.rcp.images.Images;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UI {

	private UI() {
	}

	/**
	 * Calls the given function when the browser has loaded the given URL. No
	 * threads are spawned here. You have to make sure that the given function
	 * accesses the browser in the UI thread.
	 */
	public static void onLoaded(Browser browser, String url, Runnable fn) {
		if (browser == null || url == null)
			return;
		browser.addProgressListener(new ProgressListener() {

			@Override
			public void completed(ProgressEvent event) {
				if (fn != null) {
					fn.run();
					browser.removeProgressListener(this);
				}
			}

			@Override
			public void changed(ProgressEvent event) {
			}
		});
		browser.setUrl(url);
	}

	/**
	 * Bind the given function with the given name to the `window` object of the
	 * given browser.
	 */
	public static void bindFunction(Browser browser, String name,
																	Function<Object[], Object> fn) {
		if (browser == null || name == null || fn == null)
			return;
		BrowserFunction func = new BrowserFunction(browser, name) {
			@Override
			public Object function(Object[] args) {
				try {
					return fn.apply(args);
				} catch (Exception e) {
					Logger log = LoggerFactory.getLogger(UI.class);
					log.error("failed to execute browser function " + name, e);
					return null;
				}
			}
		};
		browser.addDisposeListener(e -> {
			if (!func.isDisposed()) {
				func.dispose();
			}
		});
	}

	public static Shell shell() {
		// first, we try to get the shell from the active workbench window
		Shell shell = null;
		try {
			IWorkbenchWindow wb = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			if (wb != null) {
				shell = wb.getShell();
			}
			if (shell != null)
				return shell;
		} catch (Exception ignored) {
		}

		// then, try to get it from the display
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		if (display != null) {
			shell = display.getActiveShell();
		}
		if (shell != null)
			return shell;
		return display != null
				? new Shell(display)
				: new Shell();
	}

	public static Point initialSizeOf(
			IShellProvider dialog, int maxWidth, int maxHeight) {
		if (dialog == null)
			return new Point(maxWidth, maxHeight);
		var displaySize = dialog.getShell().getDisplay().getBounds();
		int width = displaySize.x > 0 && displaySize.x < maxWidth
				? displaySize.x
				: maxWidth;
		int height = displaySize.y > 0 && displaySize.y < maxHeight
				? displaySize.y
				: maxHeight;
		return new Point(width, height);
	}

	public static Font boldFont() {
		return JFaceResources.getFontRegistry().getBold(
				JFaceResources.DEFAULT_FONT);
	}

	public static Font italicFont() {
		return JFaceResources.getFontRegistry().getItalic(
				JFaceResources.DEFAULT_FONT);
	}

	public static Font defaultFont() {
		return JFaceResources.getFontRegistry().defaultFont();
	}

	public static void center(Shell parent, Shell child) {
		Rectangle shellBounds = parent.getBounds();
		Point size = child.getSize();
		int diffX = (shellBounds.width - size.x) / 2;
		int diffY = (shellBounds.height - size.y) / 2;
		child.setLocation(shellBounds.x + diffX, shellBounds.y + diffY);
	}

	public static GridData gridData(Control control, boolean hFill,
			boolean vFill) {
		int hStyle = hFill ? SWT.FILL : SWT.LEFT;
		int vStyle = vFill ? SWT.FILL : SWT.CENTER;
		var data = new GridData(hStyle, vStyle, hFill, vFill);
		if (hFill && (control instanceof Text
				|| control instanceof Table
				|| control instanceof Tree)) {
			// fix for this bug:
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=215997#c4
			data.widthHint = 1;
		}
		control.setLayoutData(data);
		return data;
	}

	public static GridData fillHorizontal(Control control) {
		return gridData(control, true, false);
	}

	public static ScrolledForm formHeader(ModelPage<?> page) {
		var image = Images.get(page.getEditor().getModel());
		var form = formHeader(page.getManagedForm(), page.getFormTitle(), image);
		// "" is 'general' comment on data set
		if (page.getEditor().hasComment("")) {
			form.getToolBarManager()
					.add(new CommentAction("", page.getEditor().getComments()));
		}
		Editors.addRefresh(form, page.getEditor());

		form.setBackground(Colors.formBackground());
		form.setForeground(Colors.formForeground());

		form.getToolBarManager().update(true);
		return form;
	}

	/**
	 * Creates a nice form header with the given title and returns the form.
	 */
	public static ScrolledForm formHeader(IManagedForm mform, String title) {
		return formHeader(mform, title, null);
	}

	public static ScrolledForm formHeader(
			IManagedForm mform, String title, Image image) {
		var form = mform.getForm();
		var tk = mform.getToolkit();
		tk.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		if (title != null) {
			form.setText(title);
		}
		if (image != null) {
			form.setImage(image);
		}
		form.setForeground(Colors.formForeground());
		form.setBackground(Colors.formBackground());
		// tk.decorateFormHeading(form.getForm());
		return form;
	}

	public static List formList(Composite comp) {
		var list = new org.eclipse.swt.widgets.List(comp, SWT.BORDER);
		list.setBackground(Colors.formBackground());
		list.setForeground(Colors.formForeground());
		return list;
	}

	public static Composite wizardHeader(IManagedForm mform, FormToolkit toolkit,
			String title, String description) {
		var form = UI.formHeader(mform, title);
		// setting the widget colors
		form.setBackground(Colors.wizardBackground());
		form.setForeground(Colors.wizardForeground());
		var body = form.getBody();
		UI.gridLayout(body, 1, 0, 0);
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, false);
		var descriptionComposite = toolkit.createComposite(body);
		descriptionComposite.setForeground(Colors.wizardForeground());
		descriptionComposite.setBackground(Colors.wizardBackground());
		UI.gridLayout(descriptionComposite, 1).marginTop = 0;
		UI.gridData(descriptionComposite, true, false);
		UI.wizardLabel(descriptionComposite, description);
		var separator = new Label(body, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return body;
	}

	public static Composite formSection(
			Composite parent, FormToolkit tk, String label) {
		return formSection(parent, tk, label, 2);
	}

	public static Composite formSection(Composite parent, FormToolkit tk,
			String label, int columns) {
		Section section = section(parent, tk, label);
		return sectionClient(section, tk, columns);
	}

	public static Section section(Composite comp, FormToolkit tk, String title) {
		var s = tk.createSection(comp,
				ExpandableComposite.SHORT_TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE
						| ExpandableComposite.EXPANDED
						| ExpandableComposite.TWISTIE);
		gridData(s, true, false);
		s.setText(title);

		s.setTitleBarBackground(Colors.titleBackground());
		s.setTitleBarBorderColor(Colors.titleBorder());
		s.setTitleBarForeground(Colors.titleForeground());
		s.setToggleColor(Colors.sectionToggle());

		s.setBackground(Colors.formBackground());
		s.setForeground(Colors.formForeground());
		return s;
	}

	/**
	 * Creates a composite and sets it as section client of the given section.
	 * The created composite gets a 2-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit toolkit) {
		return sectionClient(section, toolkit, 2);
	}

	/**
	 * Creates a composite and sets it as section client of the given section.
	 * The created composite gets a n-column grid-layout.
	 */
	public static Composite sectionClient(Section section, FormToolkit tk,
			int columns) {
		var composite = UI.formComposite(section, tk);
		section.setClient(composite);
		gridLayout(composite, columns);

		composite.setBackground(Colors.formBackground());
		composite.setForeground(Colors.formForeground());

		return composite;
	}

	public static void bodyLayout(Composite comp, FormToolkit tk) {
		GridLayout layout = new GridLayout();
		layout.marginRight = 10;
		layout.marginLeft = 10;
		layout.horizontalSpacing = 10;
		layout.marginBottom = 10;
		layout.marginTop = 10;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		comp.setLayout(layout);
		tk.paintBordersFor(comp);
		gridData(comp, true, true);
	}
	public static Composite formBody(ScrolledForm form, FormToolkit tk) {
		Composite body = form.getBody();
		bodyLayout(body, tk);

		body.setBackground(Colors.formBackground());
		body.setForeground(Colors.formForeground());

		return body;
	}

	public static GridLayout gridLayout(Composite composite, int columns) {
		return gridLayout(composite, columns, 10, 10);
	}

	public static GridLayout gridLayout(Composite composite, int columns,
																			int spacing, int margin) {
		var layout = new GridLayout(columns, false);
		layout.verticalSpacing = spacing;
		layout.marginWidth = margin;
		layout.marginHeight = margin;
		layout.horizontalSpacing = spacing;
		composite.setLayout(layout);
		return layout;
	}

	public static Composite formComposite(Composite parent) {
		return formComposite(parent, SWT.NONE);
	}

	public static Composite formComposite(Composite parent, int style) {
		return formComposite(parent, null, style);
	}

	public static Composite formComposite(Composite parent, FormToolkit tk) {
		return formComposite(parent, tk, SWT.NONE);
	}

	public static Composite formComposite(Composite parent, FormToolkit tk,
			int style) {
		var comp = tk == null
				? new Composite(parent, style)
				: tk.createComposite(parent, style);
		gridLayout(comp, 2);
		comp.setBackground(Colors.formBackground());
		comp.setForeground(Colors.formForeground());
		return comp;
	}

	public static Composite wizardComposite(Composite parent) {
		return wizardComposite(parent, null, SWT.NONE);
	}

	public static Composite wizardComposite(Composite parent, int style) {
		return wizardComposite(parent, null, style);
	}

	public static Composite wizardComposite(Composite parent, FormToolkit tk,
			int style) {
		var comp = tk == null
				? new Composite(parent, style)
				: tk.createComposite(parent, style);
		gridLayout(comp, 2);
		comp.setBackground(Colors.wizardBackground());
		comp.setForeground(Colors.wizardForeground());
		return comp;
	}

	public static Button formButton(Composite comp, FormToolkit tk, String text) {
		return formButton(comp, tk, text, SWT.NONE);
	}

	public static Button formButton(Composite comp, FormToolkit tk, String text,
			int style) {
		var button = tk.createButton(comp, text, style);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	public static Button button(Composite comp, int style) {
		var button = new Button(comp, style);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	/**
	 * Creates a simple check box with the given text.
	 */
	public static Button wizardCheckBox(Composite comp, FormToolkit tk, String text) {
		Button button = tk == null
				? new Button(comp, SWT.CHECK)
				: tk.createButton(comp, text, SWT.CHECK);
		button.setText(text);
		button.setBackground(Colors.wizardBackground());
		button.setForeground(Colors.wizardForeground());
		return button;
	}

	public static Button wizardCheckBox(Composite comp, String text) {
		return wizardCheckBox(comp, null, text);
	}

	/**
	 * Creates a check box as one component.
	 */
	public static Button formCheckBox(Composite parent, String label) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(label);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	/**
	 * Creates a label and check box as two separate components.
	 */
	public static Button formCheckBox(Composite parent, FormToolkit toolkit, String label) {
		formLabel(parent, toolkit, label);
		return formCheckBox(parent, toolkit);
	}

	public static Button formCheckBox(Composite parent, FormToolkit toolkit) {
		var button = toolkit != null
				? toolkit.createButton(parent, null, SWT.CHECK)
				: new Button(parent, SWT.CHECK);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	/**
	 * Create a radio button with a label.
	 */
	public static Button formRadio(Composite parent, String label) {
		var button = new Button(parent, SWT.RADIO);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		formLabel(parent, label);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	/**
	 * Create a radio button.
	 */
	public static Button formRadio(Composite parent) {
		var button = new Button(parent, SWT.RADIO);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	/**
	 * Create a radio button.
	 */
	public static Button formRadio(Composite parent, FormToolkit tk, String label) {
		var button = tk.createButton(parent, label, SWT.RADIO);
		button.setBackground(Colors.formBackground());
		button.setForeground(Colors.formForeground());
		return button;
	}

	public static Button wizardRadio(Composite parent) {
		var button = new Button(parent, SWT.RADIO);
		button.setBackground(Colors.wizardBackground());
		button.setForeground(Colors.wizardForeground());
		return button;
	}

	public static Button wizardRadio(Composite parent, String label) {
		var button = new Button(parent, SWT.RADIO);
		var gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		button.setLayoutData(gd);
		wizardLabel(parent, label);
		button.setBackground(Colors.wizardBackground());
		button.setForeground(Colors.wizardForeground());
		return button;
	}

	public static Scale wizardScale(Composite comp) {
		var scale = new Scale(comp, SWT.NONE);
		scale.setBackground(Colors.wizardBackground());
		scale.setForeground(Colors.wizardForeground());
		return scale;
	}

	public static Text formText(Composite parent, int style) {
		return formText(parent, null, null, style | SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit tk) {
		return formText(parent, tk, null, SWT.BORDER);
	}

	public static Text formText(Composite parent, String label) {
		return formText(parent, null, label);
	}

	public static Text formText(Composite parent, String label, int style) {
		return formText(parent, null, label, style | SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit toolkit,
			String label) {
		return formText(parent, toolkit, label, SWT.BORDER);
	}

	public static Text formText(Composite parent, FormToolkit tk,
			String label, int style) {
		if (label != null)
			formLabel(parent, tk, label);
		Text text = tk != null
				? tk.createText(parent, null, style)
				: new Text(parent, style);
		fillHorizontal(text);
		text.setBackground(Colors.formBackground());
		text.setForeground(Colors.formForeground());
		return text;
	}

	public static FormText formText(Composite parent, FormToolkit tk, boolean trackFocus) {
		var text = tk.createFormText(parent, trackFocus);
		text.setBackground(Colors.formBackground());
		text.setForeground(Colors.formForeground());
		return text;
	}

	public static Text wizardText(Composite parent) {
		return wizardText(parent, null, SWT.BORDER);
	}

	public static Text wizardText(Composite parent, String label) {
		return wizardText(parent, label, SWT.BORDER);
	}

	public static Text wizardText(Composite parent, int style) {
		return wizardText(parent, null, style | SWT.BORDER);
	}

	public static Text wizardText(Composite parent, String label, int style) {
		if (label != null)
			wizardLabel(parent, label);
		var text = new Text(parent, style);
		fillHorizontal(text);
		text.setBackground(Colors.wizardBackground());
		text.setForeground(Colors.wizardForeground());
		return text;
	}

	public static Text formEmptyText(Composite parent, FormToolkit toolkit) {
		return formEmptyText(parent, toolkit, SWT.NONE);
	}

	public static Text formEmptyText(Composite parent, int style) {
		return formEmptyText(parent, null, style);
	}

	public static Text formEmptyText(Composite parent, FormToolkit toolkit, int style) {
		var text = toolkit == null
				? new Text(parent, style)
				: UI.formText(parent, toolkit, "", style);
		text.setBackground(Colors.formBackground());
		text.setForeground(Colors.formForeground());
		return text;
	}

	public static Text wizardEmptyText(Composite parent, int style) {
		var text = new Text(parent, style);
		text.setBackground(Colors.wizardBackground());
		text.setForeground(Colors.wizardForeground());
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

	public static Text formMultiText(Composite comp, FormToolkit tk, int heightHint) {
		Text text = tk != null
				? tk.createText(comp, null, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI)
				: new Text(comp, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		GridData gd = fillHorizontal(text);
		gd.minimumHeight = heightHint;
		gd.heightHint = heightHint;

		text.setBackground(Colors.formBackground());
		text.setForeground(Colors.formForeground());

		return text;
	}

	public static Text wizardMultiText(Composite comp, String label) {
		wizardLabel(comp, label);
		return wizardMultiText(comp);
	}

	public static Text wizardMultiText(Composite comp) {
		var text = new Text(comp, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
		GridData gd = fillHorizontal(text);
		gd.minimumHeight = 100;
		gd.heightHint = 100;

		text.setBackground(Colors.wizardBackground());
		text.setForeground(Colors.wizardForeground());

		return text;
	}

	public static Combo formCombo(Composite comp, String label) {
		return formCombo(comp, null, label);
	}

	public static Combo formCombo(Composite comp, FormToolkit tk, String label) {
		formLabel(comp, tk, label);
		var combo = new Combo(comp, SWT.READ_ONLY);
		combo.setBackground(Colors.formBackground());
		combo.setForeground(Colors.formForeground());
		gridData(combo, true, false);
		return combo;
	}

	public static Combo wizardCombo(Composite comp, FormToolkit tk) {
		var combo = new Combo(comp, SWT.READ_ONLY);
		combo.setBackground(Colors.formBackground());
		combo.setForeground(Colors.formForeground());
		gridData(combo, true, false);
		return combo;
	}

	public static TableCombo formTableCombo(Composite comp, FormToolkit tk, int style) {
		var combo = new TableCombo(comp, style);

		if (tk != null)
			tk.adapt(combo);
		combo.setBackground(Colors.formBackground());
		combo.setForeground(Colors.formForeground());

		return combo;
	}

	public static Label formLabel(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setBackground(Colors.formBackground());
		label.setForeground(Colors.formForeground());
		return label;
	}

	public static Label formLabel(Composite comp, String text) {
		return formLabel(comp, null, text);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text) {
		return formLabel(comp, tk, text, SWT.NONE);
	}

	public static Label formLabel(Composite comp, FormToolkit tk, String text, int flag) {
		Label label;
		var s = text != null ? text : "";
		if (tk != null) {
			label = tk.createLabel(comp, s, flag);
		} else {
			label = new Label(comp, flag);
			label.setText(s);
		}
		var gd = gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		label.setBackground(Colors.formBackground());
		label.setForeground(Colors.formForeground());

		return label;
	}

	public static Label wizardLabel(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setBackground(Colors.wizardBackground());
		label.setForeground(Colors.wizardForeground());
		return label;
	}

	public static Label wizardLabel(Composite parent, String s) {
		var label = new Label(parent, SWT.NONE);
		label.setText(s);
		var gd = gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		label.setBackground(Colors.wizardBackground());
		label.setForeground(Colors.wizardForeground());

		return label;
	}

	public static CLabel formCLabel(Composite parent, FormToolkit tk, int style) {
		var cLabel = new CLabel(parent, style);
		tk.adapt(cLabel);
		cLabel.setBackground(Colors.formBackground());
		cLabel.setForeground(Colors.formForeground());
		return cLabel;
	}


	public static CLabel formCLabel(Composite parent, FormToolkit tk) {
		return formCLabel(parent, tk, SWT.NONE);
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label formFiller(Composite comp, FormToolkit tk) {
		return formLabel(comp, tk, "");
	}

	/**
	 * Creates an empty label which can be used to fill cells in a grid layout.
	 */
	public static Label formFiller(Composite comp) {
		return formLabel(comp, null, "");
	}

	public static Label wizardFiller(Composite comp) {
		return wizardLabel(comp, "");
	}

	public static Hyperlink formLink(Composite comp, FormToolkit tk, String text) {
		Hyperlink link;
		if (tk != null)
			link = tk.createHyperlink(comp, text, SWT.NONE);
		else {
			link = new Hyperlink(comp, SWT.NONE);
			link.setText(text);
		}
		GridData gd = gridData(link, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;

		link.setBackground(Colors.formBackground());
		return link;
	}

	public static ImageHyperlink formCategoryLink(Composite comp, FormToolkit tk, String category,
																								Image image) {
		var link = tk.createImageHyperlink(comp, SWT.NONE);
		link.setImage(image);
		link.setText(Strings.cutMid(category, 100));
		link.setToolTipText(category);
		link.setBackground(Colors.formBackground());
		return link;
	}

	public static ImageHyperlink formImageHyperlink(Composite comp,
			FormToolkit tk, int style) {
		var link = tk.createImageHyperlink(comp, style);
		link.setForeground(Colors.linkBlue());
		link.setBackground(Colors.formBackground());
		return link;
	}

	public static ImageHyperlink formImageHyperlink(Composite comp, FormToolkit tk) {
		return formImageHyperlink(comp, tk, SWT.NONE);
	}

	public static Spinner formSpinner(Composite comp, FormToolkit tk, int style) {
		var spinner = new Spinner(comp, style);
		tk.adapt(spinner);
		spinner.setBackground(Colors.formBackground());
		spinner.setForeground(Colors.formForeground());
		return spinner;
	}

	public static Group formGroup(Composite comp, FormToolkit tk, int style) {
		var group = new Group(comp, style);
		if (tk != null)
			tk.adapt(group);
		group.setBackground(Colors.formBackground());
		group.setForeground(Colors.formForeground());
		return group;
	}

	public static Group formGroup(Composite comp, FormToolkit tk) {
		return formGroup(comp, tk, SWT.NONE);
	}

	public static Group wizardGroup(Composite comp, int style) {
		var group = new Group(comp, style);
		group.setBackground(Colors.wizardBackground());
		group.setForeground(Colors.wizardForeground());
		return group;
	}

	public static Hyperlink formHyperlink(Composite comp, FormToolkit tk) {
		var link = new Hyperlink(comp, SWT.NONE);
		if (tk != null)
			tk.adapt(link);
		link.setBackground(Colors.formBackground());
		link.setForeground(Colors.formForeground());
		return link;
	}

	public static Hyperlink formHyperlink(Composite comp) {
		return formHyperlink(comp, null);
	}

	public static StyledText formStyledText(Composite comp, FormToolkit tk) {
		var text = new StyledText(comp, SWT.BORDER);
		tk.adapt(text);
		text.setBackground(Colors.formBackground());
		text.setForeground(Colors.formForeground());
		return text;
	}

}
