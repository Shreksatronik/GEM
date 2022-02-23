package ru.nucodelabs.gem.view.main;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import ru.nucodelabs.data.ves.ExperimentalData;
import ru.nucodelabs.gem.core.ViewManager;
import ru.nucodelabs.gem.core.utils.OSDetector;
import ru.nucodelabs.gem.model.Section;
import ru.nucodelabs.gem.view.*;
import ru.nucodelabs.gem.view.usercontrols.vescurves.VESCurves;
import ru.nucodelabs.gem.view.usercontrols.vestables.tablelines.ExperimentalTableLine;
import ru.nucodelabs.gem.view.usercontrols.vestables.tablelines.ModelTableLine;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static java.lang.Math.abs;

public class MainViewController extends Controller implements Initializable {

    /**
     * Constants
     */
    private static final int EXP_CURVE_SERIES_CNT = 3;
    private static final int THEOR_CURVE_SERIES_CNT = 4;
    private static final int MOD_CURVE_SERIES_CNT = 5;
    private static final int EXP_CURVE_SERIES_INDEX = 0;
    private static final int EXP_CURVE_ERROR_UPPER_SERIES_INDEX = 1;
    private static final int EXP_CURVE_ERROR_LOWER_SERIES_INDEX = 2;
    private static final int THEOR_CURVE_SERIES_INDEX = THEOR_CURVE_SERIES_CNT - 1;
    private static final int MOD_CURVE_SERIES_INDEX = MOD_CURVE_SERIES_CNT - 1;

    /**
     * Service-objects
     */
    private ModelCurveDragger modelCurveDragger;
    private final VESCurvesNavigator vesCurvesNavigator;
    private final ViewManager viewManager;

    /**
     * Properties
     */
    private final ObjectProperty<ObservableList<XYChart.Series<Double, Double>>> vesCurvesData;
    private final StringProperty vesTitle;
    private final StringProperty vesNumber;
    private final ObjectProperty<ObservableList<XYChart.Series<Double, Double>>> misfitStacksData;
    private final DoubleProperty vesCurvesXLowerBound;
    private final DoubleProperty vesCurvesXUpperBound;
    private final DoubleProperty vesCurvesYLowerBound;
    private final DoubleProperty vesCurvesYUpperBound;
    private final ObjectProperty<ObservableList<ExperimentalTableLine>> expTableData;
    private final ObjectProperty<ObservableList<ModelTableLine>> modelTableData;
    private final BooleanProperty noFileOpened;
    private final IntegerProperty currentPicket;

    /**
     * Data models
     */
    private final Section section;

    /**
     * Initialization
     *
     * @param viewManager View Manager
     * @param section     VES Data
     */
    public MainViewController(ViewManager viewManager, Section section) {
        this.viewManager = viewManager;
        this.section = section;

        currentPicket = new SimpleIntegerProperty(-1);

        vesCurvesXLowerBound = new SimpleDoubleProperty(-1);
        vesCurvesXUpperBound = new SimpleDoubleProperty(4);
        vesCurvesYLowerBound = new SimpleDoubleProperty(-1);
        vesCurvesYUpperBound = new SimpleDoubleProperty(4);
        vesCurvesNavigator = new VESCurvesNavigator(
                vesCurvesXLowerBound, vesCurvesXUpperBound,
                vesCurvesYLowerBound, vesCurvesYUpperBound,
                0.1
        );

        noFileOpened = new SimpleBooleanProperty(true);

        vesCurvesData = new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
        for (int i = 0; i < MOD_CURVE_SERIES_CNT; i++) {
            vesCurvesData.get().add(new XYChart.Series<>());
        }

        vesTitle = new SimpleStringProperty("");

        misfitStacksData = new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));

        expTableData = new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
        modelTableData = new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
        vesNumber = new SimpleStringProperty("0/0");
    }

    @FXML
    public VESCurves vesCurves;
    @FXML
    public MenuBar menuBar;
    @FXML
    public Menu menuView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        modelCurveDragger = new ModelCurveDragger((pointInScene) ->
                new XYChart.Data<>(
                        (Double) vesCurves
                                .getLineChartXAxis()
                                .getValueForDisplay(
                                        vesCurves.getLineChartXAxis().sceneToLocal(pointInScene).getX()
                                ),
                        (Double) vesCurves
                                .getLineChartYAxis()
                                .getValueForDisplay(
                                        vesCurves.getLineChartYAxis().sceneToLocal(pointInScene).getY()
                                )
                ),
                vesCurvesData,
                MOD_CURVE_SERIES_INDEX
        );
        if (new OSDetector().isMacOS()) {
            ResourceBundle uiProps = ResourceBundle.getBundle("ru/nucodelabs/gem/UI");
            CheckMenuItem useSystemMenu = new CheckMenuItem(uiProps.getString("useSystemMenu"));
            menuView.getItems().add(0, useSystemMenu);
            useSystemMenu.selectedProperty().bindBidirectional(menuBar.useSystemMenuBarProperty());
        }
    }

    public void closeFile() {
        viewManager.close(this);
        viewManager.start();
    }

    /**
     * Asks which EXP files and then imports them to current window
     */
    public void importEXP() {
        List<File> files = viewManager.showOpenEXPFileChooser(this);
        if (files != null && files.size() != 0) {
            for (var file : files) {
                addEXP(file);
            }
        }
    }

    private void addEXP(File file) {
        try {
            section.loadExperimentalDataFromEXPFile(currentPicket.get() + 1, file);
        } catch (Exception e) {
            viewManager.alertIncorrectFile(this, e);
        }
        currentPicket.set(currentPicket.get() + 1);
        compatibilityModeAlert();
        updateAll();
    }

    public void openSection() {
        File file = viewManager.showOpenJsonFileChooser(this);
        if (file != null) {
            try {
                section.loadFromJson(file);
            } catch (Exception e) {
                viewManager.alertIncorrectFile(this, e);
            }
            currentPicket.set(0);
            updateAll();
        }
    }

    public void saveSection() {
        File file = viewManager.showSaveJsonFileChooser(this);
        if (file != null) {
            try {
                section.saveToJson(file);
            } catch (Exception e) {
                viewManager.alertIncorrectFile(this, e);
            }
        }
    }

    /**
     * Adds files names to vesText
     */
    private void updateVESText() {
        vesTitle.set(section.getName(currentPicket.get()));
    }

    private void updateVESNumber() {
        vesNumber.set(currentPicket.get() + 1 + "/" + section.getPicketsCount());
    }

    /**
     * Warns about compatibility mode if data is unsafe
     */
    private void compatibilityModeAlert() {
        ExperimentalData experimentalData = section.getPicket(currentPicket.get()).getExperimentalData();
        if (experimentalData.isUnsafe()) {
            viewManager.alertExperimentalDataIsUnsafe(this, section.getPicket(currentPicket.get()).getName());
        }
    }

    /**
     * Opens new window
     */
    public void newWindow() {
        viewManager.start();
    }

    /**
     * Asks which file to import and then import it
     */
    public void importMOD() {
        File file = viewManager.showOpenMODFileChooser(this);

        if (file == null) {
            return;
        }

        try {
            section.loadModelDataFromMODFile(currentPicket.get(), file);
        } catch (Exception e) {
            viewManager.alertIncorrectFile(this, e);
        }

        updateAll();
    }

    private void updateAll() {
        if (section.getPicketsCount() > 0) {
            noFileOpened.set(false);
        }
        updateExpTable();
        updateExpCurves();
        updateTheoreticalCurve();
        updateModelCurve();
        updateModelTable();
        updateMisfitStacks();
        updateVESText();
        updateVESNumber();
    }

    public void switchToNextPicket() {
        if (section.getPicketsCount() > currentPicket.get() + 1) {
            currentPicket.set(currentPicket.get() + 1);
            updateAll();
        }
    }

    public void switchToPrevPicket() {
        if (currentPicket.get() > 0 && section.getPicketsCount() > 0) {
            currentPicket.set(currentPicket.get() - 1);
            updateAll();
        }
    }

    public void zoomInVesCurves() {
        vesCurvesNavigator.zoomIn();
    }

    public void zoomOutVesCurves() {
        vesCurvesNavigator.zoomOut();
    }

    public void moveLeftVesCurves() {
        vesCurvesNavigator.moveLeft();
    }

    public void moveRightVesCurves() {
        vesCurvesNavigator.moveRight();
    }

    public void moveUpVesCurves() {
        vesCurvesNavigator.moveUp();
    }

    public void moveDownVesCurves() {
        vesCurvesNavigator.moveDown();
    }

    private void updateTheoreticalCurve() {
        XYChart.Series<Double, Double> theorCurveSeries = new XYChart.Series<>();

        if (section.getModelData(currentPicket.get()) != null) {
            try {
                theorCurveSeries = VESSeriesConverters.toTheoreticalCurveSeries(
                        section.getExperimentalData(currentPicket.get()), section.getModelData(currentPicket.get())
                );
            } catch (UnsatisfiedLinkError e) {
                viewManager.alertNoLib(this, e);
            }
        }

        vesCurvesData.get().set(THEOR_CURVE_SERIES_INDEX, theorCurveSeries);
    }

    private void updateModelCurve() {
        XYChart.Series<Double, Double> modelCurveSeries = new XYChart.Series<>();

        if (section.getModelData(currentPicket.get()) != null) {
            modelCurveSeries = VESSeriesConverters.toModelCurveSeries(
                    section.getModelData(currentPicket.get())
            );
            vesCurvesData.get().set(MOD_CURVE_SERIES_INDEX, modelCurveSeries);
            addDraggingToModelCurveSeries(modelCurveSeries);
        } else {
            vesCurvesData.get().set(MOD_CURVE_SERIES_INDEX, modelCurveSeries);
        }
    }

    private void addDraggingToModelCurveSeries(XYChart.Series<Double, Double> modelCurveSeries) {
        try {
            modelCurveDragger.mapModelData(section.getModelData(currentPicket.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        modelCurveSeries.getNode().setCursor(Cursor.HAND);
        modelCurveSeries.getNode().setOnMousePressed(e -> modelCurveDragger.lineToDragDetector(e));
        modelCurveSeries.getNode().setOnMouseDragged(e -> {
            modelCurveDragger.dragHandler(e);
            updateMisfitStacks();
            updateTheoreticalCurve();
            updateModelTable();
        });
    }

    private void updateExpCurves() {
        XYChart.Series<Double, Double> expCurveSeries = VESSeriesConverters.toExperimentalCurveSeries(
                section.getExperimentalData(currentPicket.get())
        );
        XYChart.Series<Double, Double> errUpperExp = VESSeriesConverters.toErrorExperimentalCurveUpperBoundSeries(
                section.getExperimentalData(currentPicket.get())
        );
        XYChart.Series<Double, Double> errLowerExp = VESSeriesConverters.toErrorExperimentalCurveLowerBoundSeries(
                section.getExperimentalData(currentPicket.get())
        );
        vesCurvesData.get().set(EXP_CURVE_SERIES_INDEX, expCurveSeries);
        vesCurvesData.get().set(EXP_CURVE_ERROR_UPPER_SERIES_INDEX, errUpperExp);
        vesCurvesData.get().set(EXP_CURVE_ERROR_LOWER_SERIES_INDEX, errLowerExp);
        if (section.getModelData(currentPicket.get()) == null) {
            vesCurvesData.get().set(THEOR_CURVE_SERIES_INDEX, new XYChart.Series<>());
            vesCurvesData.get().set(MOD_CURVE_SERIES_INDEX, new XYChart.Series<>());
        }
    }

    private void updateExpTable() {
        expTableData.setValue(
                VESTablesConverters.toExperimentalTableData(
                        section.getExperimentalData(currentPicket.get())
                )
        );
    }

    private void updateModelTable() {
        ObservableList<ModelTableLine> modelTableLines = FXCollections.emptyObservableList();

        if (section.getModelData(currentPicket.get()) != null) {
            modelTableLines = VESTablesConverters.toModelTableData(
                    section.getModelData(currentPicket.get())
            );
        }

        modelTableData.setValue(modelTableLines);
    }

    private void updateMisfitStacks() {
        List<XYChart.Series<Double, Double>> misfitStacksSeriesList = new ArrayList<>();

        if (section.getModelData(currentPicket.get()) != null) {
            try {
                misfitStacksSeriesList = MisfitStacksSeriesConverters.toMisfitStacksSeriesList(
                        section.getExperimentalData(currentPicket.get()), section.getModelData(currentPicket.get())
                );
            } catch (UnsatisfiedLinkError e) {
                viewManager.alertNoLib(this, e);
            }
        }

        misfitStacksData.get().clear();
        misfitStacksData.get().addAll(misfitStacksSeriesList);
        colorizeMisfitStacksSeries();
    }

    /**
     * Colorizes misfit stacks with green and red, green for ones that <100%, red for ≥100%
     */
    private void colorizeMisfitStacksSeries() {
        var data = misfitStacksData.get();
        for (var series : data) {
            var nonZeroPoint = series.getData().get(1);
            if (abs(nonZeroPoint.getYValue()) < 100f) {
                series.getNode().setStyle("-fx-stroke: LimeGreen;");
                nonZeroPoint.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: LimeGreen");
                var zeroPoint = series.getData().get(0);
                zeroPoint.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: LimeGreen");
            }
        }
    }

    public ObservableList<XYChart.Series<Double, Double>> getVesCurvesData() {
        return vesCurvesData.get();
    }

    public ObjectProperty<ObservableList<XYChart.Series<Double, Double>>> vesCurvesDataProperty() {
        return vesCurvesData;
    }

    public ObservableList<XYChart.Series<Double, Double>> getMisfitStacksData() {
        return misfitStacksData.get();
    }

    public ObjectProperty<ObservableList<XYChart.Series<Double, Double>>> misfitStacksDataProperty() {
        return misfitStacksData;
    }

    public String getVesTitle() {
        return vesTitle.get();
    }

    public StringProperty vesTitleProperty() {
        return vesTitle;
    }

    public double getVesCurvesXLowerBound() {
        return vesCurvesXLowerBound.get();
    }

    public DoubleProperty vesCurvesXLowerBoundProperty() {
        return vesCurvesXLowerBound;
    }

    public double getVesCurvesXUpperBound() {
        return vesCurvesXUpperBound.get();
    }

    public DoubleProperty vesCurvesXUpperBoundProperty() {
        return vesCurvesXUpperBound;
    }

    public double getVesCurvesYLowerBound() {
        return vesCurvesYLowerBound.get();
    }

    public DoubleProperty vesCurvesYLowerBoundProperty() {
        return vesCurvesYLowerBound;
    }

    public double getVesCurvesYUpperBound() {
        return vesCurvesYUpperBound.get();
    }

    public DoubleProperty vesCurvesYUpperBoundProperty() {
        return vesCurvesYUpperBound;
    }

    public ObservableList<ExperimentalTableLine> getExpTableData() {
        return expTableData.get();
    }

    public ObjectProperty<ObservableList<ExperimentalTableLine>> expTableDataProperty() {
        return expTableData;
    }

    public ObservableList<ModelTableLine> getModelTableData() {
        return modelTableData.get();
    }

    public ObjectProperty<ObservableList<ModelTableLine>> modelTableDataProperty() {
        return modelTableData;
    }

    public boolean getNoFileOpened() {
        return noFileOpened.get();
    }

    public BooleanProperty noFileOpenedProperty() {
        return noFileOpened;
    }

    public int getCurrentPicket() {
        return currentPicket.get();
    }

    public IntegerProperty currentPicketProperty() {
        return currentPicket;
    }

    public String getVesNumber() {
        return vesNumber.get();
    }

    public StringProperty vesNumberProperty() {
        return vesNumber;
    }
}
