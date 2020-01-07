package org.openlca.app.editors.lcia_methods.shapefiles;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.editors.lcia_methods.ImpactCategoryEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.tables.Tables;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SFParameterTable {

	TableViewer viewer;
	List<ShapeFileParameter> params;

	SFParameterTable(ImpactCategoryEditor editor, String shapeFile, Composite parent) {
		viewer = Tables.createViewer(parent, M.Name, M.Minimum, M.Maximum);
		viewer.setLabelProvider(new Label());
		Tables.bindColumnWidths(viewer, 0.4, 0.3, 0.3);
		try {
			params = ShapeFileUtils.getParameters(editor.getModel(), shapeFile);
			viewer.setInput(params);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to read parameteres for shape file " + shapeFile, e);
		}
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int i) {
			if (i == 0)
				return Images.get(ModelType.PARAMETER);
			else
				return null;
		}

		@Override
		public String getColumnText(Object o, int i) {
			if (!(o instanceof ShapeFileParameter))
				return null;
			ShapeFileParameter p = (ShapeFileParameter) o;
			switch (i) {
			case 0:
				return p.name;
			case 1:
				return Double.toString(p.min);
			case 2:
				return Double.toString(p.max);
			default:
				return null;
			}
		}
	}
}