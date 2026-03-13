package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SystemBinding;

import java.util.Objects;

public final class SystemNode extends SdNode {

	private final SystemBinding binding;

	public SystemNode(SystemBinding binding) {
		this.binding = Objects.requireNonNull(binding);
		var v = binding.view();
		var b = v != null
			? new Rectangle(v.x(), v.y(), v.width(), v.height())
			: new Rectangle(100, 100, 80, 40);
		moveTo(b);
	}
	public SystemBinding binding() {
		return binding;
	}

	public void moveTo(Rectangle rect) {
		if (rect == null) return;
		binding.setView(new Rect(rect.x, rect.y, rect.width, rect.height));
		super.moveTo(rect);
	}
}
