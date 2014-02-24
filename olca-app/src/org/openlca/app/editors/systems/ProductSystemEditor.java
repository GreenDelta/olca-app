package org.openlca.app.editors.systems;

import org.openlca.app.Messages;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemEditor extends ModelEditor<ProductSystem> implements
		IEditor {

	public static String ID = "editors.productsystem";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ProductSystemEditor() {
		super(ProductSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProductSystemInfoPage(this));
			addPage(new ProductSystemParameterPage(this));
			BaseDescriptor descriptor = getEditorInput().getDescriptor();
			GraphicalEditorInput gInput = new GraphicalEditorInput(descriptor);
			int gIdx = addPage(new ProductSystemGraphEditor(getModel(), this),
					gInput);
			setPageText(gIdx, Messages.ModelGraph);
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}
}
