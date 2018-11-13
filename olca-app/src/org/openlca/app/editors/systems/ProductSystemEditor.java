package org.openlca.app.editors.systems;

import org.openlca.app.M;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemEditor extends ModelEditor<ProductSystem> {

	public static String ID = "editors.productsystem";
	private Logger log = LoggerFactory.getLogger(getClass());
	private ProductSystemInfoPage infoPage;
	private ProductSystemParameterPage parameterPage;

	public ProductSystemEditor() {
		super(ProductSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			infoPage = new ProductSystemInfoPage(this);
			parameterPage = new ProductSystemParameterPage(this);
			addPage(infoPage);
			addPage(parameterPage);
			BaseDescriptor descriptor = getEditorInput().getDescriptor();
			GraphicalEditorInput gInput = new GraphicalEditorInput(descriptor);
			int gIdx = addPage(new ProductSystemGraphEditor(this), gInput);
			setPageText(gIdx, M.ModelGraph);
			if (FeatureFlag.EXPERIMENTAL_VISUALISATIONS.isEnabled()) {
				addPage(new StatisticsPage(this));
			}
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
