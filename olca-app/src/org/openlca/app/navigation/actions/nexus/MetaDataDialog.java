package org.openlca.app.navigation.actions.nexus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.nexus.Types.AggregationType;
import org.openlca.app.navigation.actions.nexus.Types.BiogenicCarbonModeling;
import org.openlca.app.navigation.actions.nexus.Types.CarbonStorageModeling;
import org.openlca.app.navigation.actions.nexus.Types.EmissionModeling;
import org.openlca.app.navigation.actions.nexus.Types.EndOfLifeModeling;
import org.openlca.app.navigation.actions.nexus.Types.InfrastructureModeling;
import org.openlca.app.navigation.actions.nexus.Types.ModelingType;
import org.openlca.app.navigation.actions.nexus.Types.MultifunctionalModeling;
import org.openlca.app.navigation.actions.nexus.Types.RepresentativenessType;
import org.openlca.app.navigation.actions.nexus.Types.ReviewSystem;
import org.openlca.app.navigation.actions.nexus.Types.ReviewType;
import org.openlca.app.navigation.actions.nexus.Types.SourceReliability;
import org.openlca.app.navigation.actions.nexus.Types.WaterModeling;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.ProductSystemDao;

class MetaDataDialog extends FormDialog {

	private final MetaData metaData = new MetaData();

	MetaDataDialog() {
		super(UI.shell());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 900);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				"Specify database wide meta data (* -> multiple values can be separated by a comma ',')");
		var toolkit = mform.getToolkit();
		var body = UI.dialogBody(form, toolkit);

		var general = UI.formSection(body, toolkit, "General meta data");
		createMultiString(general, toolkit, "Nomenclature",
				v -> metaData.supportedNomenclatures = v);
		createMultiString(general, toolkit, "Impact methods",
				v -> metaData.lciaMethods = v);
		createSelect(general, toolkit, "Inventory modeling type", ModelingType.class,
				v -> metaData.modelingType = v);
		createSelect(general, toolkit, "Multifunctional modeling", MultifunctionalModeling.class,
				v -> metaData.multifunctionalModeling = v);
		createSelect(general, toolkit, "Biogenice carbon modeling", BiogenicCarbonModeling.class,
				v -> metaData.biogenicCarbonModeling = v);
		createSelect(general, toolkit, "End of life modeling", EndOfLifeModeling.class,
				v -> metaData.endOfLifeModeling = v);
		createSelect(general, toolkit, "Water modeling", WaterModeling.class,
				v -> metaData.waterModeling = v);
		createSelect(general, toolkit, "Infrastructure modeling", InfrastructureModeling.class,
				v -> metaData.infrastructureModeling = v);
		createSelect(general, toolkit, "Emission modeling", EmissionModeling.class,
				v -> metaData.emissionModeling = v);
		createSelect(general, toolkit, "Carbon storage modeling", CarbonStorageModeling.class,
				v -> metaData.carbonStorageModeling = v);
		createSelect(general, toolkit, "Review type", ReviewType.class,
				v -> metaData.reviewType = v);
		createSelect(general, toolkit, "Review system", ReviewSystem.class,
				v -> metaData.reviewSystem = v);

		var process = UI.formSection(body, toolkit, "Process meta data");
		createSelect(process, toolkit, "Representativeness type", RepresentativenessType.class,
				v -> metaData.representativenessType = v);
		createSelect(process, toolkit, "Source reliability", SourceReliability.class,
				v -> metaData.sourceReliability = v);
		createSelect(process, toolkit, "Aggregation type", AggregationType.class,
				v -> metaData.aggregationType = v);
		createMultiString(process, toolkit, "System model",
				v -> metaData.systemModel = v);

		if (!new ProductSystemDao(Database.get()).getDescriptors().isEmpty()) {
			var system = UI.formSection(body, toolkit, "Product system meta data");
			var check = UI.labeledCheckbox(system, toolkit, "Export product systems");
			check.setSelection(true);
			Controls.onSelect(check, $ -> metaData.exportSystems = check.getSelection());
			createString(system, toolkit, "Creator",
					v -> metaData.creator = v);
			createString(system, toolkit, "Intended audience",
					v -> metaData.intendedAudience = v);
		}

		form.reflow(true);
	}

	private void createMultiString(Composite parent, FormToolkit toolkit, String label, Consumer<List<String>> setter) {
		var text = UI.labeledText(parent, toolkit, label + "*");
		text.addModifyListener($ -> setter.accept(Arrays.asList(text.getText().split(","))));
	}

	private void createString(Composite parent, FormToolkit toolkit, String label, Consumer<String> setter) {
		var text = UI.labeledText(parent, toolkit, label);
		text.addModifyListener($ -> setter.accept(text.getText()));
	}

	private <T extends Enum<?>> void createSelect(Composite parent, FormToolkit toolkit, String label, Class<T> clazz,
			Consumer<List<T>> setter) {
		UI.label(parent, toolkit, label);
		var combo = new EnumCombo<T>(parent, clazz);
		combo.addSelectionChangedListener(v -> setter.accept(Collections.singletonList(v)));
	}

	public MetaData getMetaData() {
		return metaData;
	}

	public class EnumCombo<T extends Enum<?>> extends AbstractComboViewer<T> {

		private final Class<T> clazz;

		public EnumCombo(Composite parent, Class<T> clazz) {
			super(parent);
			this.clazz = clazz;
			setInput(clazz.getEnumConstants());
		}

		@Override
		public Class<T> getType() {
			return clazz;
		}

	}
}
