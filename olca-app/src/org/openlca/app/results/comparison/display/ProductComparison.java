package org.openlca.app.results.comparison.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.db.Database;
import org.openlca.app.results.ProjectResultEditor;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.Category;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;

public class ProductComparison {
	private Composite shell;
	private List<Contributions> contributionsList;
	private Point screenSize;
	private Config config;
	private Point origin;
	private final Point margin;
	private final int productHeight;
	private final int gapBetweenProduct;
	private int theoreticalScreenHeight;
	private ColorCellCriteria colorCellCriteria;
	private Map<Integer, Image> cacheMap;
	private Combo selectCategory;
	private Color chosenCategoryColor;
	private ContributionResult contributionResult;
	private List<ContributionResult> contributionResults;
	private int nonCutoffAmount;
	private int cutOffSize;
	private IDatabase db;
	private ProductSystem productSystem;
	private ImpactMethodDescriptor impactMethod;
	private Map<String, ImpactDescriptor> impactCategoryMap;
	private List<String> impactCategoriesName;
	private TargetCalculationEnum targetCalculation;
	private Composite composite;
	private boolean isCalculationStarted;
	private long chosenProcessCategoryId;
	private Map<Integer, Map<Integer, List<Contribution<CategorizedDescriptor>>>> contributionResultMap;
	private Button runCalculation;
	private FormToolkit tk;
	private List<ProjectVariant> variants;

	public ProductComparison(Composite shell, Config config, ResultEditor<?> editor, FormToolkit tk) {
		this.tk = tk;
		db = Database.get();
		if (editor.result == null) {
			targetCalculation = TargetCalculationEnum.PRODUCT;
			contributionResults = ((ProjectResultEditor) editor).results;
			variants = ((ProjectResultEditor) editor).variants;

		} else {
			targetCalculation = TargetCalculationEnum.IMPACT;
		}
		productSystem = editor.setup.productSystem;
		impactMethod = editor.setup.impactMethod;
		contributionResult = editor.result;
		this.shell = shell;
		this.config = config;
		colorCellCriteria = config.colorCellCriteria;
		targetCalculation = config.targetCalculationCriteria;
		contributionsList = new ArrayList<>();
		cacheMap = new HashMap<>();
		contributionResultMap = new HashMap<>();
		impactCategoriesName = new ArrayList<>();
		chosenProcessCategoryId = -1;
		margin = new Point(200, 65);
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = margin.y * 2 + gapBetweenProduct * 2;
		nonCutoffAmount = 100;
		cutOffSize = 25;
		origin = new Point(0, 0);
		isCalculationStarted = false;
	}

	/**
	 * Entry point of the program. Display the products, and draw links between each
	 * matching results
	 */
	public void display() {
		Contributions.config = config;
		Contributions.updateComparisonCriteria(colorCellCriteria);
		Cell.config = config;

		Section settingsSection = UI.section(shell, tk, "Settings");
		Composite comp = UI.sectionClient(settingsSection, tk);
		UI.gridLayout(comp, 1);

		/**
		 * Composite component
		 */
		var row1 = tk.createComposite(comp);

		var row2 = tk.createComposite(shell);
//		UI.gridLayout(row2, 1);
		row2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		row2.setLayout(new GridLayout(1, false));

		/**
		 * Canvas component
		 */
		var canvas = new Canvas(row2, SWT.V_SCROLL);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/**
		 * VBar component
		 */
		var vBar = canvas.getVerticalBar();
		vBar.setMinimum(0);

		addScrollListener(canvas);
		addResizeEvent(row2, canvas);

		row1.setLayout(new RowLayout());

//		chooseTargetMenu(row1, row2);
//		chooseImpactCategoriesMenu(row1, row2);
		if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
			impactCategoryMap = contributionResult.getImpacts().stream().sorted((c1, c2) -> c1.name.compareTo(c2.name))
					.map(impactCategory -> {
						impactCategoriesName.add(impactCategory.name);
						return impactCategory;
					})
					.collect(Collectors.toMap(impactCategory -> impactCategory.name, impactCategory -> impactCategory));
		}else {
			impactCategoryMap = contributionResults.stream().map(result -> {
				result.getImpacts().stream().sorted((c1, c2) -> c1.name.compareTo(c2.name)).map(impactCategory -> {
					impactCategoriesName.add(impactCategory.name);
					return impactCategory;
				}).collect(Collectors.toList());
			}).collect(Collectors.toMap(impactCategory -> impactCategory.name, impactCategory -> impactCategory));
		}
		UI.gridLayout(row1, 10);

		colorByCriteriaMenu(row1, row2, canvas);
		colorPickerMenu(row1);
		selectCategoryMenu(row1, row2, canvas);
		selectCutoffSizeMenu(row1, row2, canvas);
		selectAmountVisibleProcessMenu(row1, row2, canvas);
		runCalculationButton(row1, row2, canvas);

		addPaintListener(canvas); // Once finished, we really paint the cache, so it avoids flickering
		runCalculation.notifyListeners(SWT.Selection, new Event());

	}

	/**
	 * Allow to choose a target : either display contributions results for each
	 * impact category, or for each product system
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void chooseTargetMenu(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setBounds(0, 0, 0, 0);
		l.setVisible(false);
		l.setText("Target : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setVisible(false);
		c.setBounds(0, 0, 0, 0);
//		c.setBounds(50, 50, 150, 65);
		String values[] = TargetCalculationEnum.valuesToString();
		c.setItems(values);
		var index = ArrayUtils.indexOf(values, targetCalculation.toString());
		c.select(index);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				var choice = c.getItem(c.getSelectionIndex());
				TargetCalculationEnum criteria = TargetCalculationEnum.getTarget(choice);
				if (!targetCalculation.equals(criteria)) {
					targetCalculation = criteria;
					impactCategoryTable(composite);
				}
			}
		});
	}

	/**
	 * Dropdown menu, allow us to chose different Impact Categories
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void chooseImpactCategoriesMenu(Composite row1, Composite row2) {
		var b = new Button(row1, SWT.NONE);
		b.setVisible(false);
		composite = new Composite(row1, SWT.BORDER);
		composite.setVisible(false);
		var impactCategoryTable = impactCategoryTable(composite);
		impactCategoryMap = contributionResult.getImpacts().stream().sorted((c1, c2) -> c1.name.compareTo(c2.name))
				.map(impactCategory -> {
					TableItem item = new TableItem(impactCategoryTable, SWT.BORDER);
					item.setText(impactCategory.id + ": " + impactCategory.name);
					item.setChecked(true);
					impactCategoriesName.add(item.getText());
					return impactCategory;
				}).collect(Collectors.toMap(impactCategory -> impactCategory.id + ": " + impactCategory.name,
						impactCategory -> impactCategory));
		b.setText("Toggle");
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Arrays.stream(impactCategoryTable.getItems()).forEach(item -> {
					item.setChecked(!item.getChecked());
					if (item.getChecked()) {
						impactCategoriesName.add(item.getText());
					} else {
						impactCategoriesName.remove(item.getText());
					}
				});
			}
		});
		b.pack();
		impactCategoryTable.setBounds(0, 0, 0, 0);
//		impactCategoryTable.setSize(300, 100);
//		row1.setSize(300, 100);
	}

	/**
	 * Table containing the whole impact categories, that allow us to check them or
	 * not
	 * 
	 * @param composite The parent component
	 * @return This table
	 */
	private Table impactCategoryTable(Composite composite) {
		int typeButton;
		if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
			typeButton = SWT.CHECK;
		} else {
			typeButton = SWT.RADIO;
		}
		var impactCategoryTable = new Table(composite, typeButton | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		impactCategoryTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					// We keep tracking of check/select event, to have in impactCategoriesName the
					// checked impact categories
					if (((TableItem) event.item).getChecked()) {
						impactCategoriesName.add(((TableItem) event.item).getText());
					} else {
						impactCategoriesName.remove(((TableItem) event.item).getText());
					}
				} else { // Select event
					var checked = ((TableItem) event.item).getChecked();
					((TableItem) event.item).setChecked(!checked);
					if (!checked) {
						impactCategoriesName.add(((TableItem) event.item).getText());
					} else {
						impactCategoriesName.remove(((TableItem) event.item).getText());
					}
				}
			}
		});
		return impactCategoryTable;
	}

	/**
	 * Dropdown menu, allow us to chose by what criteria we want to color the cells
	 * : either by product (default), product category or location
	 * 
	 * @param row1   The menu bar
	 * @param row2   The second part of the display
	 * @param canvas The canvas
	 */
	private void colorByCriteriaMenu(Composite row1, Composite row2, Canvas canvas) {
		final Combo c = UI.formCombo(row1, "Color cells by : ");
		c.setBounds(50, 50, 150, 65);
		String values[] = ColorCellCriteria.valuesToString();
		c.setItems(values);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				var choice = c.getItem(c.getSelectionIndex());
				ColorCellCriteria criteria = ColorCellCriteria.getCriteria(choice);
				if (!colorCellCriteria.equals(criteria)) {
					colorCellCriteria = criteria;
					Contributions.updateComparisonCriteria(criteria);
					contributionsList.stream().forEach(p -> p.updateCellsColor());
					triggerComboSelection(selectCategory, true);
					redraw(row2, canvas);
				}
			}
		});
	}

	/**
	 * Dropdown menu, allow us to chose a specific Process Category to color
	 * 
	 * @param row1   The menu bar
	 * @param row2   The second part of the display
	 * @param canvas The canvas
	 */
	private void selectCategoryMenu(Composite row1, Composite row2, Canvas canvas) {
		selectCategory = UI.formCombo(row1, "Select Product Category : ");
//		selectCategory.setBounds(50, 50, 550, 65);
		var<String, Descriptor> categoryMap = new HashMap<String, Descriptor>();
		selectCategory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectCategory.getSelectionIndex() == -1) { // Nothing is selected : initialisation
					chosenProcessCategoryId = -1;
					resetDefaultColorCells();
					var<String> list = contributionsList.stream()
							.flatMap(p -> p.getList().stream().flatMap(results -> results.getResult().stream()
									.filter(r -> r.getContribution().item != null).map(r -> {
										var categoryId = r.getContribution().item.category;
										var cat = db.getDescriptor(Category.class, categoryId);
										if (categoryMap.get(cat.name) == null) {
											categoryMap.put(cat.name, cat);
										}
										return cat.name;
									})))
							.distinct().sorted().collect(Collectors.toList());
					list.add(0, "");
					selectCategory.setItems(list.toArray(String[]::new));
				} else if (selectCategory.getSelectionIndex() == 0) { // Empty value is selected : reset
					chosenProcessCategoryId = 0;
					resetDefaultColorCells();
					redraw(row2, canvas);
				} else { // A category is selected : update color
					resetDefaultColorCells();
					chosenProcessCategoryId = categoryMap
							.get(selectCategory.getItem(selectCategory.getSelectionIndex())).id;
					contributionsList.stream().forEach(p -> p.getList().stream().forEach(c -> {
						if (c.getResult().get(0).getContribution().item.category == chosenProcessCategoryId) {
							c.setRgb(chosenCategoryColor.getRGB());
						}
					}));
					redraw(row2, canvas);
				}
			}
		});
	}

	/**
	 * Trigger a selection event to a combo component
	 * 
	 * @param deselect Indicate if we deselect the selected value of the combo
	 */
	private void triggerComboSelection(Combo combo, boolean deselect) {
		if (deselect) {
			combo.deselectAll();
		}
		Event event = new Event();
		event.widget = combo;
		event.display = combo.getDisplay();
		event.type = SWT.Selection;
		combo.notifyListeners(SWT.Selection, event);
	}

	/**
	 * Reset the default color of the cells
	 */
	public void resetDefaultColorCells() {
		RGB rgb = chosenCategoryColor.getRGB();
		// Reset categories colors to default (just for the one which where changed)
		contributionsList.stream().forEach(p -> p.getList().stream().filter(cell -> cell.getRgb().equals(rgb))
				.forEach(cell -> cell.resetDefaultRGB()));
	}

	/**
	 * The swt widget that allows to pick a custom color
	 * 
	 * @param composite The parent component
	 */
	private void colorPickerMenu(Composite composite) {
		// Default color (pink)
		chosenCategoryColor = new Color(shell.getDisplay(), new RGB(255, 0, 255));
		// Use a label full of spaces to show the color
		final Label colorLabel = new Label(composite, SWT.NONE);
		colorLabel.setText("    ");
		colorLabel.setBackground(chosenCategoryColor);
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Color...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				// Create the color-change dialog
				ColorDialog dlg = new ColorDialog(shell.getShell());
				// Set the selected color in the dialog from
				// user's selected color
				dlg.setRGB(colorLabel.getBackground().getRGB());
				// Change the title bar text
				dlg.setText("Choose a Color");
				// Open the dialog and retrieve the selected color
				RGB rgb = dlg.open();
				if (rgb != null) {
					// Dispose the old color, create the
					// new one, and set into the label
					chosenCategoryColor.dispose();
					chosenCategoryColor = new Color(composite.getDisplay(), rgb);
					colorLabel.setBackground(chosenCategoryColor);
					triggerComboSelection(selectCategory, false);
				}
			}
		});
	}

	/**
	 * Spinner allowing to set the ratio of the cutoff area
	 * 
	 * @param row1   The menu bar
	 * @param row2   The second part of the display
	 * @param canvas The canvas
	 */
	private void selectCutoffSizeMenu(Composite row1, Composite row2, Canvas canvas) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select cutoff size (%): ");
		var selectCutoff = new Spinner(row1, SWT.BORDER);
		selectCutoff.setBounds(50, 50, 500, 65);
		selectCutoff.setMinimum(0);
		selectCutoff.setMaximum(100);
		selectCutoff.setSelection(cutOffSize);
		selectCutoff.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (e.keyCode == 13) { // If we press Enter
					cutOffSize = selectCutoff.getSelection();
					redraw(row2, canvas);
				}
			}
		});
	}

	/**
	 * Spinner allowing to set the amount of visible process
	 * 
	 * @param row1   The menu bar
	 * @param row2   The second part of the display
	 * @param canvas The canvas
	 */
	private void selectAmountVisibleProcessMenu(Composite row1, Composite row2, Canvas canvas) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select amount non cutoff process: ");
		var selectCutoff = new Spinner(row1, SWT.BORDER);
		selectCutoff.setBounds(50, 50, 500, 65);
		selectCutoff.setMinimum(0);
		selectCutoff.setMaximum(10000);
		selectCutoff.setSelection(nonCutoffAmount);
		selectCutoff.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (arg0.keyCode == 13) { // If we press Enter
					nonCutoffAmount = selectCutoff.getSelection();
					redraw(row2, canvas);
				}
			}
		});
	}

	/**
	 * Run the calculation, according to the selected values
	 * 
	 * @param row1   The menu bar
	 * @param row2   The second part of the display
	 * @param canvas The canvas
	 */
	private void runCalculationButton(Composite row1, Composite row2, Canvas canvas) {
		var vBar = canvas.getVerticalBar();
		runCalculation = new Button(row1, SWT.None);
		runCalculation.setVisible(false);
		runCalculation.setText("Run calculation");
		runCalculation.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				System.out.println("Run calc");
				contributionsList = new ArrayList<>();
				if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
					var productSystemResults = contributionResultMap.get(productSystem.hashCode());
					if (productSystemResults == null) {
						productSystemResults = new HashMap<>();
						contributionResultMap.put(productSystem.hashCode(), productSystemResults);
					}
					impactCategoriesName.stream().forEach(item -> {
						var psResults = contributionResultMap.get(productSystem.hashCode());
						var impactDescriptor = impactCategoryMap.get(item);
						var contributionList = psResults.get(impactDescriptor.hashCode());
						if (contributionList == null) {
							contributionList = contributionResult.getProcessContributions(impactDescriptor);
							psResults.put(impactDescriptor.hashCode(), contributionList); // We cache the process
																							// contributions
						}
						var p = new Contributions(contributionList, item, null);
						contributionsList.add(p);
					});
				} else {
					var wrapper = new Object() {
						int count = 0;
					};
					contributionResults.forEach(result -> {
						var impactDescriptor = impactCategoryMap.get(impactCategoriesName.get(0));
						var contributionList = result.getProcessContributions(impactDescriptor);
						var ps = variants.get(wrapper.count).productSystem;
						var p = new Contributions(contributionList, impactDescriptor.name, ps.id + ": " + ps.name);
						contributionsList.add(p);
						wrapper.count++;

					});
//					new ProductSystemDao(db).getAll().stream().forEach(ps -> {
//						var psResults = contributionResultMap.get(ps.hashCode());
//						if (psResults == null) {
//							psResults = new HashMap<>();
//							contributionResultMap.put(ps.hashCode(), psResults);
//						}
//						var impactDescriptor = impactCategoryMap.get(impactCategoriesName.get(0));
//						var contributionList = psResults.get(impactDescriptor.hashCode());
//						if (contributionList == null) {
//							var setup = new CalculationSetup(ps);
//							setup.impactMethod = impactMethod;
//							var calc = new SystemCalculator(db);
//							var fullResult = calc.calculateFull(setup);
//							contributionList = fullResult.getProcessContributions(impactDescriptor);
//							psResults.put(impactDescriptor.hashCode(), contributionList); // We cache the process
//																							// contribution
//						}
//						var p = new Contributions(contributionList, impactDescriptor.name, ps.id + ": " + ps.name);
//						contributionsList.add(p);
//					});
				}
				isCalculationStarted = true;
				theoreticalScreenHeight = margin.y * 2 + gapBetweenProduct * (contributionsList.size() - 1);
				vBar.setMaximum(theoreticalScreenHeight);
				sortProducts();
				triggerComboSelection(selectCategory, true);
				redraw(row2, canvas);
			}
		});
	}

	/**
	 * Sort products by ascending amount, according to the comparison criteria
	 */
	private void sortProducts() {
		Contributions.updateComparisonCriteria(colorCellCriteria);
		contributionsList.stream().forEach(p -> p.sort());
	}

	/**
	 * Redraw everything
	 * 
	 * @param composite The parent component
	 * @param canvas    The canvas
	 */
	private void redraw(Composite composite, Canvas canvas) {
		var vBar = canvas.getVerticalBar();
		screenSize = shell.getSize();
		if (screenSize.y == 0) {
			return;
		}
		var hash = computeConfigurationHash();
		// Cached image, in which we draw the things, and then display it once it is
		// finished
		Image cache = cacheMap.get(hash);
		if (cache == null) { // Otherwise, we create it, and cache it
			cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
			cachedPaint(composite, cache); // Costly painting, so we cache it
			var newHash = computeConfigurationHash();
			cacheMap.put(newHash, cache);
		}
		Rectangle client = canvas.getClientArea();
		vBar.setThumb(Math.min(theoreticalScreenHeight, client.height));
		vBar.setPageIncrement(Math.min(theoreticalScreenHeight, client.height));
		vBar.setIncrement(20);
		canvas.redraw();
	}

	/**
	 * Compute a hash from the different configuration element : the target, the
	 * impact categories name, etc
	 * 
	 * @return A hash
	 */
	private int computeConfigurationHash() {
		var hash = Objects.hash(targetCalculation, impactCategoriesName, chosenCategoryColor, colorCellCriteria,
				cutOffSize, nonCutoffAmount, shell.getSize(), isCalculationStarted, chosenProcessCategoryId);
		return hash;
	}

	/**
	 * Costly painting method. For each process contributions, it draws links
	 * between each matching results. Since it is costly, it is firstly drawed on an
	 * image. Once it is finished, we paint the image
	 * 
	 * @param composite The parent component
	 * @param cache     The cached image in which we are drawing
	 */
	private void cachedPaint(Composite composite, Image cache) {
		GC gc = new GC(cache);
		screenSize = shell.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.85; // 85% of the screen width
		// Starting point of the first product rectangle
		Point rectEdge = new Point(0 + margin.x, 0 + margin.y);
		var optional = contributionsList.stream()
				.mapToDouble(p -> p.getList().stream().mapToDouble(c -> c.getNormalizedAmount()).sum()).max();
		double maxSumAmount = 0.0;
		if (optional.isPresent()) {
			maxSumAmount = optional.getAsDouble();
		}
		for (int productIndex = 0; productIndex < contributionsList.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex, maxSumAmount);
			rectEdge = new Point(rectEdge.x, rectEdge.y + 300);
		}
		drawLinks(gc);
	}

	/**
	 * Handle the current product. Draw a rectangle, write the product name in it,
	 * and handle the product results
	 * 
	 * @param gc              The GC component
	 * @param maxProductWidth The maximal width for a product
	 * @param rectEdge        The coordinate of the product rectangle
	 * @param productIndex    The index of the current product
	 * @param maxSumAmount    The max amounts sum of the products
	 */
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex, double maxSumAmount) {
		var p = contributionsList.get(productIndex);
		int productWidth = (int) maxProductWidth;
		// Draw the product name
		Point textPos = new Point(rectEdge.x - margin.x, rectEdge.y + 6);
		gc.drawText("Contribution result " + (productIndex + 1), textPos.x, textPos.y);
		textPos.y += 27;
		gc.drawText("Impact : " + p.getImpactCategoryName(), textPos.x, textPos.y);
		productWidth = handleCells(gc, rectEdge, productIndex, p, productWidth, maxSumAmount);

		// Draw an arrow above the first product contributions to show the way the
		// results are ordered
		if (productIndex == 0) {
			Point startPoint = new Point(rectEdge.x, rectEdge.y - 50);
			Point endPoint = new Point((int) (startPoint.x + maxProductWidth), startPoint.y);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y + 15);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y - 15);
			drawLine(gc, startPoint, endPoint, null, null);
		}
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, productWidth, productHeight);
	}

	/**
	 * Handle the cells, and display a rectangle for each of them (and merge the
	 * cutoff one in one visual cell)
	 * 
	 * @param gc           The GC component
	 * @param rectEdge     The coordinate of the product rectangle
	 * @param productIndex The index of the current product
	 * @param p            The current product
	 * @param productWidth The product width
	 * @param maxAmount    The max amounts sum of the products
	 * @return The new product width
	 */
	private int handleCells(GC gc, Point rectEdge, int productIndex, Contributions p, int productWidth,
			double maxSumAmount) {
		var cells = p.getList();
		long amountCutOff = cells.size() - nonCutoffAmount;
		long cutOffProcessAmount = cells.stream().skip(amountCutOff).filter(c -> c.getAmount() == 0.0).count();
		cutOffProcessAmount += amountCutOff;
		int newProductWidth = handleCutOff(cells, productWidth, rectEdge, gc, cutOffProcessAmount);
		return newProductWidth;
	}

	/**
	 * Handle the non focused values : they are in the cutoff area if we don't want
	 * them to be display (by choosing an amount of process to be display), or if
	 * they are null contributions
	 * 
	 * @param cells                 The list of cells
	 * @param remainingProductWidth The remaining width where we can draw
	 * @param rectEdge              The corner of the product rectangle
	 * @param gc                    The GC component
	 * @param cutOffProcessAmount   The amount of process in the cutoff area
	 * @return The total width of the product
	 */
	private int handleCutOff(List<Cell> cells, int remainingProductWidth, Point rectEdge, GC gc,
			long cutOffProcessAmount) {
		Point start = new Point(rectEdge.x + 1, rectEdge.y + 1);
		if (cutOffSize == 0) {
			return handleNotBigEnoughProcess(cells, remainingProductWidth, rectEdge, gc, cutOffProcessAmount, start, 0);
		}
		RGB rgbCutOff = new RGB(192, 192, 192); // Color for cutoff area
		double cutoffRectangleSizeRatio = cutOffSize / 100.0;
		int productCutOffWidth = 0;
		if (cutOffProcessAmount == cells.size()) {
			productCutOffWidth = remainingProductWidth;
		} else {
			productCutOffWidth = (int) (remainingProductWidth * cutoffRectangleSizeRatio);
		}
		double normalizedCutOffAmountSum = cells.stream().limit(cutOffProcessAmount)
				.mapToDouble(cell -> Math.abs(cell.getNormalizedAmount())).sum();
		double minimumGapBetweenCells = ((double) productCutOffWidth / cutOffProcessAmount);
		int chunk = -1, chunkSize = 0, newChunk = 0;
		boolean gapBigEnough = true;
		if (minimumGapBetweenCells < 1.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / minimumGapBetweenCells);
			gapBigEnough = false;
		}
		Point end = null;
		var newProductWidth = 0;
		for (var cellIndex = 0; cellIndex < cutOffProcessAmount; cellIndex++) {
			if (!gapBigEnough) {
				newChunk = computeChunk(chunk, chunkSize, cellIndex);
			}
			var cell = cells.get(cellIndex);
			int cellWidth = 0;
			if (!gapBigEnough && chunk != newChunk) {
				// We are on a new chunk, so we draw a cell with a width of 1 pixel
				cellWidth = 1;
			} else if (!gapBigEnough && chunk == newChunk) {
				// We stay on the same chunk, so we don't draw the cell
				cellWidth = 0;
			} else {
				var value = cell.getNormalizedAmount();
				var percentage = value / normalizedCutOffAmountSum;
				cellWidth = (int) (productCutOffWidth * percentage);
			}
			newProductWidth += cellWidth;
			end = computeEndCell(start, cell, (int) cellWidth, true);
			if (gapBigEnough || !gapBigEnough && chunk != newChunk) {
				// We end the current chunk / cell
				start = end;
				chunk = newChunk;
			}
		}
		if (cutOffProcessAmount == cells.size()) {
			newProductWidth = productCutOffWidth;
		}
		fillRectangle(gc, new Point(rectEdge.x + 1, rectEdge.y + 1), newProductWidth, productHeight - 1, rgbCutOff,
				SWT.COLOR_WHITE);
		return handleNotBigEnoughProcess(cells, (int) (remainingProductWidth - newProductWidth), rectEdge, gc,
				cutOffProcessAmount, (end != null) ? end : start, (int) newProductWidth);
	}

	/**
	 * Handle non cutoff area, but too small values, so we display them with a
	 * minimum width
	 * 
	 * @param cells                 The list of cells
	 * @param remainingProductWidth The remaining product width where we can draw
	 * @param rectEdge              The corner of the product rectangle
	 * @param gc                    The GC component
	 * @param cutOffProcessAmount   The amount of cutoff Process
	 * @param start                 The starting point
	 * @param currentProductWidth   The current product width
	 * @return The new product total width
	 */
	private int handleNotBigEnoughProcess(List<Cell> cells, int remainingProductWidth, Point rectEdge, GC gc,
			long cutOffProcessAmount, Point start, int currentProductWidth) {
		int minCellWidth = 3;
		double nonCutOffSum = cells.stream().skip(cutOffProcessAmount).mapToDouble(c -> c.getNormalizedAmount()).sum();
		long notBigEnoughContributionAmount = cells.stream().skip(cutOffProcessAmount).filter(
				cell -> Math.abs(cell.getNormalizedAmount()) / nonCutOffSum * remainingProductWidth <= minCellWidth)
				.count();
		if (notBigEnoughContributionAmount == 0) {
			// If there is no too small values, we skip this part
			return handleBigEnoughProcess(cells, (int) (remainingProductWidth), rectEdge, gc, cutOffProcessAmount,
					start, currentProductWidth);
		}
		int chunk = -1, chunkSize = 0, newChunk = 0;
		boolean gapEnoughBig = true;
		Point end = null;
		double length = minCellWidth * notBigEnoughContributionAmount;
		if (length > remainingProductWidth) {
			// if the length is to big, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(((double) length) / remainingProductWidth);
			gapEnoughBig = false;
		}
		var newProductWidth = 0;
		for (var cellIndex = cutOffProcessAmount; cellIndex < cutOffProcessAmount
				+ notBigEnoughContributionAmount; cellIndex++) {
			if (!gapEnoughBig) {
				newChunk = computeChunk(chunk, chunkSize, (int) cellIndex);
			}
			var cell = cells.get((int) cellIndex);
			int cellWidth = 0;
			if (!gapEnoughBig && chunk != newChunk || gapEnoughBig) {
				// We are on a new chunk, so we draw a cell with a minimum width
				cellWidth = minCellWidth;
				fillRectangle(gc, start, cellWidth, productHeight - 1, cell.getRgb(), SWT.COLOR_WHITE);
			} else if (!gapEnoughBig && chunk == newChunk) {
				// We stay on the same chunk, so we don't draw the cell
				cellWidth = 0;
			}
			newProductWidth += cellWidth;
			end = computeEndCell(start, cell, (int) cellWidth, false);
			if (gapEnoughBig || !gapEnoughBig && chunk != newChunk) {
				// We end the current chunk / cell
				start = end;
				chunk = newChunk;
			}
		}
		return handleBigEnoughProcess(cells, (int) (remainingProductWidth - newProductWidth), rectEdge, gc,
				cutOffProcessAmount + notBigEnoughContributionAmount, (end != null) ? end : start,
				(int) (currentProductWidth + newProductWidth));
	}

	/**
	 * Handle the bigger values, by drawing cells with proportional width to the
	 * value
	 * 
	 * @param cells                 The cells list
	 * @param remainingProductWidth The remaining product width where we can draw
	 * @param rectEdge              The corner of the product rectangle
	 * @param gc                    The GC component
	 * @param currentCellIndex      The current cell index
	 * @param start                 The starting point
	 * @param currentProductWidth   The current product width
	 * @return The new product width
	 */
	private int handleBigEnoughProcess(List<Cell> cells, int remainingProductWidth, Point rectEdge, GC gc,
			long currentCellIndex, Point start, int currentProductWidth) {
		if (currentCellIndex == cells.size()) {
			return (int) currentProductWidth + remainingProductWidth;
		}
		double amountSum = cells.stream().skip(currentCellIndex).mapToDouble(c -> c.getNormalizedAmount()).sum();
		double minimumGapBetweenCells = (remainingProductWidth / (cells.size() - currentCellIndex));
		int chunk = -1, chunkSize = 0, newChunk = 0;
		boolean gapBigEnough = true;
		if (minimumGapBetweenCells < 1.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / minimumGapBetweenCells);
			gapBigEnough = false;
		}
		var newProductWidth = 0;
		for (var cellIndex = currentCellIndex; cellIndex < cells.size(); cellIndex++) {
			if (!gapBigEnough) {
				newChunk = computeChunk(chunk, chunkSize, (int) cellIndex);
			}
			var cell = cells.get((int) cellIndex);
			int cellWidth = 0;

			if (cellIndex != cells.size() - 1) {
				var percentage = cell.getNormalizedAmount() / amountSum;
				cellWidth = (int) (remainingProductWidth * percentage);
			} else {
				// For the last cell, we fill it with the remaining empty width
				cellWidth = (int) (remainingProductWidth - newProductWidth);
			}
			newProductWidth += cellWidth;
			fillRectangle(gc, start, cellWidth, productHeight - 1, cell.getRgb(), SWT.COLOR_WHITE);
			var end = computeEndCell(start, cell, (int) cellWidth, false);
			if (gapBigEnough || !gapBigEnough && chunk != newChunk) {
				// We end the current chunk / cell
				start = end;
				chunk = newChunk;
			}
		}
		return (int) currentProductWidth + remainingProductWidth;
	}

	/**
	 * Tells in which chunk we are
	 * 
	 * @param chunk       Index of the current chunk
	 * @param chunkSize   Amount of cells in a chunk
	 * @param resultIndex The cell index
	 * @return The new chunk index
	 */
	private int computeChunk(int chunk, int chunkSize, int cellIndex) {
		// Every chunkSize, we increment the chunk
		var newChunk = (cellIndex % (int) chunkSize) == 0;
		if (newChunk == true) {
			chunk++;
		}
		return chunk;
	}

	/**
	 * Compute the end of the current cell, and set some important information about
	 * the cell
	 * 
	 * @param start     The starting point of the cell
	 * @param cell      The current cell
	 * @param cellWidth The width of the cell
	 * @return The end point of the cell
	 */
	private Point computeEndCell(Point start, Cell cell, int cellWidth, boolean isCutoff) {
		var end = new Point(start.x + cellWidth, start.y);
		var startingPoint = new Point((end.x + start.x) / 2, start.y + productHeight);
		var endingPoint = new Point(startingPoint.x, start.y - 2);
		cell.setData(startingPoint, endingPoint, start.x, end.x, isCutoff);
		return end;
	}

	/**
	 * Draw a line, with an optional color
	 * 
	 * @param gc          The GC
	 * @param start       The starting point
	 * @param end         The ending point
	 * @param beforeColor The color of the line
	 * @param afterColor  The color to get back after the draw
	 */
	private void drawLine(GC gc, Point start, Point end, Object beforeColor, Integer afterColor) {
		if (beforeColor != null) {
			if (beforeColor instanceof Integer) {
				gc.setForeground(gc.getDevice().getSystemColor((int) beforeColor));
			} else {
				gc.setForeground(new Color(gc.getDevice(), (RGB) beforeColor));
			}
		}
		gc.drawLine(start.x, start.y, end.x, end.y);
		if (afterColor != null) {
			gc.setForeground(gc.getDevice().getSystemColor(afterColor));
		}
	}

	/**
	 * Draw a filled rectangle, with an optional color
	 * 
	 * @param gc          The GC
	 * @param start       The starting point
	 * @param width       The width
	 * @param height      The height
	 * @param beforeColor The color of the rectangle
	 * @param afterColor  The color to get back after the draw
	 */
	private void fillRectangle(GC gc, Point start, int width, int heigth, Object beforeColor, Integer afterColor) {
		if (beforeColor != null) {
			if (beforeColor instanceof Integer) {
				gc.setBackground(gc.getDevice().getSystemColor((int) beforeColor));
			} else {
				gc.setBackground(new Color(gc.getDevice(), (RGB) beforeColor));
			}
		}
		gc.fillRectangle(start.x, start.y, width, heigth);
		if (afterColor != null) {
			gc.setBackground(gc.getDevice().getSystemColor(afterColor));
		}
	}

	/**
	 * Draw the links between each matching results
	 * 
	 * @param gc The GC component
	 */
	private void drawLinks(GC gc) {
		for (int productIndex = 0; productIndex < contributionsList.size() - 1; productIndex++) {
			var cells = contributionsList.get(productIndex);
			for (Cell cell : cells.getList()) {
				if (cell.isLinkDrawable()) {
					var nextCells = contributionsList.get(productIndex + 1);
					// We search for a cell that has the same process
					var optional = nextCells.getList().stream()
							.filter(next -> next.getResult().get(0).getContribution().item
									.equals(cell.getResult().get(0).getContribution().item))
							.findFirst();
					if (optional.isPresent()) {
						var linkedCell = optional.get();
						var startPoint = cell.getStartingLinkPoint();
						var endPoint = linkedCell.getEndingLinkPoint();
						if (config.useBezierCurve) {
							drawBezierCurve(gc, startPoint, endPoint, cell.getRgb());
						} else {
							drawLine(gc, startPoint, endPoint, cell.getRgb(), null);
						}
					}
				}
			}
		}
	}

	/**
	 * Draw a bezier curve, between 2 points
	 * 
	 * @param gc    The GC component
	 * @param start The starting point
	 * @param end   The ending point
	 * @param rgb   The color of the curve
	 */
	private void drawBezierCurve(GC gc, Point start, Point end, RGB rgb) {
		gc.setForeground(new Color(gc.getDevice(), rgb));
		Path p = new Path(gc.getDevice());
		p.moveTo(start.x, start.y);
		int offset = 100;
		Point ctrlPoint1 = new Point(start.x + offset, start.y + offset);
		Point ctrlPoint2 = new Point(end.x - offset, end.y - offset);
		p.cubicTo(ctrlPoint1.x, ctrlPoint1.y, ctrlPoint2.x, ctrlPoint2.y, end.x, end.y);
		gc.drawPath(p);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
	}

	/**
	 * Add a scroll listener to the canvas
	 * 
	 * @param canvas The canvas component
	 */
	private void addScrollListener(Canvas canvas) {
		var vBar = canvas.getVerticalBar();
		vBar.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int vSelection = vBar.getSelection();
				int destY = -vSelection - origin.y;
				canvas.scroll(0, destY, 0, 0, canvas.getSize().x, canvas.getSize().y, false);
				origin.y = -vSelection;
			}
		});
	}

	/**
	 * Add a resize listener to the canvas
	 * 
	 * @param composite Parent composent of the canvas
	 * @param canvas    The Canvas component
	 */
	private void addResizeEvent(Composite composite, Canvas canvas) {
		var vBar = canvas.getVerticalBar();
		canvas.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				Rectangle client = canvas.getClientArea();
				vBar.setThumb(Math.min(theoreticalScreenHeight, client.height));
				vBar.setPageIncrement(Math.min(theoreticalScreenHeight, client.height));
				vBar.setIncrement(20);
				int vPage = canvas.getSize().y - client.height;
				int vSelection = vBar.getSelection();
				if (vSelection >= vPage) {
					if (vPage <= 0)
						vSelection = 0;
					origin.y = -vSelection;
				}
				redraw(composite, canvas);
			}
		});
	}

	/**
	 * Add a paint listener to the canvas. This is called whenever the canvas needs
	 * to be redrawed, then it draws the cached image
	 * 
	 * @param canvas A Canvas component
	 */
	private void addPaintListener(Canvas canvas) {
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				var hash = computeConfigurationHash();
				var cache = cacheMap.get(hash);
				e.gc.drawImage(cache, origin.x, origin.y);
			}
		});
	}
}