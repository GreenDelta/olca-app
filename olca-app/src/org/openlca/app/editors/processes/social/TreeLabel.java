package org.openlca.app.editors.processes.social;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;

class TreeLabel extends BaseLabelProvider implements ITableLabelProvider {

	private final ProcessEditor editor;

	TreeLabel(ProcessEditor editor) {
		this.editor = editor;
	}

	@Override
	public Image getColumnImage(Object obj, int col) {
		if (obj instanceof CategoryNode)
			return col == 0 ? Images.getForCategory(ModelType.SOCIAL_INDICATOR) : null;
		if (!(obj instanceof SocialAspect))
			return null;
		if (col == 0)
			return Images.get(ModelType.SOCIAL_INDICATOR);
		SocialAspect a = (SocialAspect) obj;
		if (col == 6 && a.source != null)
			return Images.get(ModelType.SOURCE);
		if (col == 7)
			return Images.get(editor.getComments(), CommentPaths.get(a));
		return null;
	}

	@Override
	public String getColumnText(Object obj, int col) {
		if (obj instanceof CategoryNode)
			return text((CategoryNode) obj, col);
		if (obj instanceof SocialAspect)
			return text((SocialAspect) obj, col);
		return null;
	}

	private String text(CategoryNode n, int col) {
		if (col == 0 && n.category != null)
			return n.category.name;
		return null;
	}

	private String text(SocialAspect a, int col) {
		switch (col) {
		case 0:
			return a.indicator != null ? a.indicator.name : null;
		case 1:
			return getRawAmount(a);
		case 2:
			return Labels.riskLevel(a.riskLevel);
		case 3:
			return getActivityValue(a);
		case 4:
			return a.quality;
		case 5:
			return a.comment;
		case 6:
			return Labels.getDisplayName(a.source);
		default:
			return null;
		}
	}

	private String getRawAmount(SocialAspect a) {
		if (a == null || a.rawAmount == null)
			return null;
		String t = a.rawAmount;
		if (a.indicator != null && a.indicator.unitOfMeasurement != null) {
			String u = a.indicator.unitOfMeasurement;
			t += " [" + u + "]";
		}
		return t;
	}

	private String getActivityValue(SocialAspect a) {
		if (a == null || a.indicator == null)
			return null;
		String t = Double.toString(a.activityValue);
		SocialIndicator i = a.indicator;
		if (i.activityVariable != null && i.activityUnit != null) {
			t += " [" + i.activityUnit.name + ", "
					+ i.activityVariable + "]";
		}
		return t;
	}

}