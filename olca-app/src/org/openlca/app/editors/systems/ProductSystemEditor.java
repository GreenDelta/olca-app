package org.openlca.app.editors.systems;

import org.openlca.app.M;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graph.GraphicalEditorInput;
import org.openlca.app.editors.graph.GraphEditor;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemEditor extends ModelEditor<ProductSystem> {

	public static String ID = "editors.productsystem";

	public ProductSystemEditor() {
		super(ProductSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProductSystemInfoPage(this));
			// addPage(new ProductSystemParameterPage(this));
			addPage(new ParameterPage2(this));
			var descriptor = getEditorInput().getDescriptor();
			GraphicalEditorInput gInput = new GraphicalEditorInput(descriptor);
			int gIdx = addPage(new GraphEditor(this), gInput);
			setPageText(gIdx, M.ModelGraph);
			addPage(new StatisticsPage(this));
			addCommentPage();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to add page", e);
		}
	}

}
