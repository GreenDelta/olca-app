package org.openlca.app;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class Messages extends NLS {

	public static String AccessAndUseRestrictions;
	public static String Actor;
	public static String Actors;
	public static String Add;
	public static String AddConnectedProcesses;
	public static String AddNewChildCategory;
	public static String AdditionalInformation;
	public static String Address;
	public static String AdministrativeInformation;
	public static String AllocationMethod;
	public static String AlreadyConnected;
	public static String AlreadyPresent;
	public static String Amount;
	public static String Analysis;
	public static String AnalysisResultOf;
	public static String AnalyzingForProblems;
	public static String AsDefinedInProcesses;
	public static String AssignUnits;
	public static String Author;
	public static String AvoidedProduct;
	public static String AvoidedProductFlow;
	public static String AvoidedWasteFlow;

	public static String Browse;
	public static String BuildNextTier;
	public static String BuildSupplyChain;

	public static String CASNumber;
	public static String Calculate;
	public static String CalculateCosts;
	public static String CalculateDefaultValues;
	public static String CalculateResults;
	public static String CalculationProperties;
	public static String CalculationType;
	public static String CalculationWizardDescription;
	public static String Category;
	public static String Causal;
	public static String CheckForUpdates;
	public static String ChooseDirectory;
	public static String City;
	public static String Code;
	public static String Collapse;
	public static String CollapseAll;
	public static String Complete;
	public static String CompleteReferenceData;
	public static String Connect;
	public static String ConnectProviders;
	public static String ConnectRecipients;
	public static String ConnectWithSystemProcessesIfPossible;
	public static String ConsumedBy;
	public static String Content;
	public static String Context;
	public static String Contribution;
	public static String ConversionFactor;
	public static String Copy;
	public static String Copyright;
	public static String Country;
	public static String CreateANewDatabase;
	public static String CreateANewProductFlowForTheProcess;
	public static String CreateDatabase;
	public static String CreateNew;
	public static String CreateProcess;
	public static String CreateProcesslink;
	public static String CreateProductSystem;
	public static String CreatesANewActor;
	public static String CreatesANewFlow;
	public static String CreatesANewFlowProperty;
	public static String CreatesANewImpactMethod;
	public static String CreatesANewProductSystem;
	public static String CreatesANewProject;
	public static String CreatesANewSource;
	public static String CreatesANewUnitGroup;
	public static String CreatingEcoSpoldFolder;
	public static String CreatingProductSystem;
	public static String CreationDate;
	public static String Cut;
	public static String CutOffForFirstLayerIn;
	public static String Cutoff;

	public static String Daily;
	public static String DataCollectionPeriod;
	public static String DataCompleteness;
	public static String DataDocumentor;
	public static String DataGenerator;
	public static String DataSelection;
	public static String DataSetOtherEvaluation;
	public static String DataSetOwner;
	public static String DataSourceInformation;
	public static String DataTreatment;
	public static String Database;
	public static String DatabaseContent;
	public static String DatabaseImport;
	public static String DatabaseImportDescription;
	public static String DatabaseName;
	public static String DefaultFlowProperty;
	public static String DefaultMethod;
	public static String DefaultProvider;
	public static String Delete;
	public static String DeleteDatabase;
	public static String DeleteProcess;
	public static String DeleteProcesslink;
	public static String Description;
	public static String Direction;
	public static String DoYouReallyWantToDelete;
	public static String Doi;

	public static String Economic;
	public static String Edit;
	public static String ElementaryFlow;
	public static String Email;
	public static String EmptyDatabase;
	public static String EndDate;
	public static String English;
	public static String Error;
	public static String Example;
	public static String Expand;
	public static String ExpandAll;
	public static String Export;
	public static String ExportAsMatrix;
	public static String ExportDatabase;
	public static String ExportEcoSpold;
	public static String ExportILCD;
	public static String ExportToExcel;
	public static String ExportingProcesses;

	public static String Factor;
	public static String File;
	public static String FileAlreadyExists;
	public static String FileImportPage_Description;
	public static String Filter;
	public static String FilterByName;
	public static String Flow;
	public static String FlowContributions;
	public static String FlowProperties;
	public static String FlowProperty;
	public static String FlowPropertyType;
	public static String FlowType;
	public static String Flows;
	public static String Formula;
	public static String FormulaEvaluationFailed;
	public static String FoundProblems;
	public static String FromDirectory;
	public static String FunctionalUnit;

	public static String GeneralInformation;
	public static String Geography;
	public static String GeometricMean;
	public static String GeometricStandardDeviation;
	public static String German;
	public static String Global;
	public static String GlobalParameters;
	public static String Goal;
	public static String GoalAndScope;
	public static String Group;
	public static String Grouping;
	public static String Groups;

	public static String Help;
	public static String Hide;
	public static String Host;
	public static String Hourly;

	public static String ImpactAssessmentMethod;
	public static String ImpactAssessmentMethods;
	public static String ImpactCategories;
	public static String ImpactCategory;
	public static String ImpactContributions;
	public static String ImpactFactors;
	public static String Import;
	public static String ImportDatabase;
	public static String ImportEcoSpold;
	public static String ImportILCD;
	public static String Information;
	public static String InfrastructureFlow;
	public static String InfrastructureProcess;
	public static String Inputs;
	public static String InputsOutputs;
	public static String IntendedApplication;
	public static String IsNotAValidNumber;
	public static String IsReference;

	public static String LCIMethod;
	public static String Language;
	public static String LastChange;
	public static String LastModificationDate;
	public static String Latitude;
	public static String Layout;
	public static String LayoutAs;
	public static String Location;
	public static String Locations;
	public static String LogNormalDistribution;
	public static String Longitude;

	public static String Map;
	public static String Mark;
	public static String Maximize;
	public static String MaximizeAll;
	public static String Maximum;
	public static String Mean;
	public static String MinimalTree;
	public static String Minimize;
	public static String MinimizeAll;
	public static String Minimum;
	public static String Mode;
	public static String ModelGraph;
	public static String ModelingAndValidation;
	public static String ModelingConstants;
	public static String MonteCarloSimulation;
	public static String Monthly;
	public static String Move;

	public static String Name;
	public static String Never;
	public static String NewActor;
	public static String NewCategory;
	public static String NewDatabase;
	public static String NewDatabase_AlreadyExists;
	public static String NewDatabase_InvalidName;
	public static String NewDatabase_NameToShort;
	public static String NewFlow;
	public static String NewFlowProperty;
	public static String NewImpactMethod;
	public static String NewLCIAMethod;
	public static String NewProcess;
	public static String NewProductSystem;
	public static String NewProject;
	public static String NewSource;
	public static String NewUnitGroup;
	public static String No;
	public static String NoAnalysisOptionsSet;
	public static String NoDescription;
	public static String NoDistribution;
	public static String NoQuantitativeReferenceSelected;
	public static String NoReferenceFlowPropertySelected;
	public static String NoReferenceProcessSelected;
	public static String NoUnitGroupSelected;
	public static String None;
	public static String NormalDistribution;
	public static String Normalization;
	public static String NormalizationAndWeightingSet;
	public static String NormalizationFactor;
	public static String NormalizationWeighting;
	public static String NormalizationWeightingSets;
	public static String Note;
	public static String NumberFormatPage_Description;
	public static String NumberOfDecimalPlaces;
	public static String NumberOfIterations;
	public static String NumberOfSimulations;

	public static String OnlineHelp;
	public static String Open;
	public static String OpenInEditor;
	public static String OpenLCAUpdateCheck;
	public static String OpenMiniatureView;
	public static String Other;
	public static String Outputs;
	public static String OverwriteFileQuestion;

	public static String Parameter;
	public static String Parameters;
	public static String Paste;
	public static String PedigreeUncertainty;
	public static String Physical;
	public static String PleaseEnterAName;
	public static String PleaseEnterANewName;
	public static String PleaseEnterTheNameOfTheNewCategory;
	public static String Prefer;
	public static String Process;
	public static String ProcessContributions;
	public static String ProcessEvaluationAndValidation;
	public static String ProcessType;
	public static String Processes;
	public static String ProducedBy;
	public static String Product;
	public static String ProductSystem;
	public static String ProductSystems;
	public static String Progress;
	public static String Project;
	public static String Projects;
	public static String Properties;
	public static String Publication;

	public static String QuantitativeReference;
	public static String QuickResults;

	public static String ReconnectProcesslink;
	public static String Reference;
	public static String ReferenceFlowProperty;
	public static String ReferenceProcess;
	public static String ReferenceUnit;
	public static String ReferenceUnitIsEmptyOrInvalid;
	public static String Reload;
	public static String RemoteDatabase;
	public static String Remove;
	public static String RemoveConnections;
	public static String RemoveObject;
	public static String RemoveSelected;
	public static String RemoveSupplyChain;
	public static String Rename;
	public static String Report;
	public static String ReportParameters;
	public static String ReserveMemoryMessage;
	public static String Reset;
	public static String Resize;
	public static String ResultContributions;
	public static String Results;
	public static String ResultsOf;
	public static String Reviewer;
	public static String Route;

	public static String SamplingProcedure;
	public static String Sankey_ScaleDescription;
	public static String Save;
	public static String SaveAs;
	public static String SaveAsDefault;
	public static String SaveAsImage;
	public static String SaveChanges;
	public static String SaveChangesQuestion;
	public static String SavingDiagramAsImageIn;
	public static String Search;
	public static String SearchParameters;
	public static String SearchProvidersFor;
	public static String SearchRecipientsFor;
	public static String Searching;
	public static String SearchingForUnits;
	public static String Select;
	public static String SelectADirectory;
	public static String SelectAUserInterfaceLanguage;
	public static String SelectImportFiles;
	public static String SelectLanguageNoteMessage;
	public static String SelectObjectPage_Description;
	public static String SelectProviders;
	public static String SelectRecipients;
	public static String SelectTheExportFile;
	public static String SelectTheParameterYouWantToReferTo;
	public static String SetSankeyDiagramOptions;
	public static String Settings;
	public static String SettingsForTheSankeyDiagram;
	public static String Show;
	public static String ShowFormulas;
	public static String ShowOutline;
	public static String ShowValues;
	public static String Showviews;
	public static String SingleAmount;
	public static String SolvingProblems;
	public static String Source;
	public static String Sources;
	public static String StandardDeviation;
	public static String Start;
	public static String StartDate;
	public static String SubCategory;
	public static String Synonyms;
	public static String SystemProcess;

	public static String TargetAmount;
	public static String Technology;
	public static String Telefax;
	public static String Telephone;
	public static String Test;
	public static String TestDistribution;
	public static String TextDropComponent_ToolTipText;
	public static String TextReference;
	public static String Time;
	public static String TimeAndAuthor;
	public static String ToDirectory;
	public static String TotalAmount;
	public static String Tree;
	public static String TriangleDistribution;
	public static String Type;

	public static String Uncertainty;
	public static String UncertaintyDistribution;
	public static String UniformDistribution;
	public static String Unit;
	public static String UnitAlreadyExistsInUnitGroup;
	public static String UnitGroup;
	public static String UnitGroups;
	public static String UnitMappingPage_Description;
	public static String UnitProcess;
	public static String Units;
	public static String UnitsAndFlowProperties;
	public static String Unknown;
	public static String Unmark;
	public static String UpdatePreferences;
	public static String Usage;
	public static String UsageOf;
	public static String UsedInProcesses;
	public static String UserFriendlyName;

	public static String Value;
	public static String Version;

	public static String Warning;
	public static String Waste;
	public static String Website;
	public static String Weekly;
	public static String Weighting;
	public static String WeightingFactor;
	public static String Window;

	public static String Year;
	public static String Yes;

	public static String ZipCode;

	private static Map<String, String> map;

	static {
		NLS.initializeMessages("org.openlca.app.messages", Messages.class);
	}

	private Messages() {
	}

	public static Map<String, String> getMap() {
		if (map == null)
			map = new HashMap<>();
		try {
			for (Field field : Messages.class.getDeclaredFields()) {
				if (!Objects.equals(field.getType(), String.class))
					continue;
				if (!Modifier.isStatic(field.getModifiers()))
					continue;
				if (!Modifier.isPublic(field.getModifiers()))
					continue;
				String val = (String) field.get(null);
				map.put(field.getName(), val);
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Messages.class);
			log.error("failed to get messages as map", e);
		}
		return map;
	}

	public static String asJson() {
		try {
			Gson gson = new Gson();
			return gson.toJson(getMap());
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Messages.class);
			log.error("failed to get messages as JSON string", e);
			return "{}";
		}
	}
}
