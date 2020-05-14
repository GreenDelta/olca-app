package org.openlca.app.editors.parameters;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.core.database.usage.ParameterUsageTree;
import org.openlca.core.model.Parameter;

public class RenameParameterDialog extends FormDialog {

    private final ParameterUsageTree usageTree;
    private final Parameter param;

    private RenameParameterDialog(
            Parameter param, ParameterUsageTree usageTree) {
        super(UI.shell());
        this.param = param;
        this.usageTree = usageTree;
    }

    public static void open(Parameter param) {
        if (param == null)
            return;
        var db = Database.get();
        var tree = new AtomicReference<ParameterUsageTree>();
		App.run(
                "Collect dependencies",
                () -> tree.set(ParameterUsageTree.build(param.name, db)),
                () -> new RenameParameterDialog(param, tree.get()).open());
    }

    @Override
    protected void createFormContent(IManagedForm mform) {
        var tk = mform.getToolkit();
        var body = UI.formBody(mform.getForm(), tk);
        var comp = UI.formComposite(body, tk);
        var text = UI.formText(comp, tk, "New name");
    }
}
