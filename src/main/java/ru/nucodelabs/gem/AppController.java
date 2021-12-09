package ru.nucodelabs.gem;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ru.nucodelabs.data.ModelData;
import ru.nucodelabs.data.Picket;
import ru.nucodelabs.files.sonet.EXPFile;
import ru.nucodelabs.files.sonet.MODFile;
import ru.nucodelabs.files.sonet.STTFile;
import ru.nucodelabs.files.sonet.SonetImport;
import ru.nucodelabs.gem.charts.InaccuracyCurve;
import ru.nucodelabs.gem.charts.VESCurve;
import ru.nucodelabs.gem.tables.ExperimentalTable;
import ru.nucodelabs.gem.tables.TableLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class AppController implements Initializable {

    Picket picket;
    VESCurve vesCurve;

    @FXML
    public VBox mainPane;
    public MenuBar menuBar;
    public MenuItem menuFileOpenEXP;
    public MenuItem menuFileOpenMOD;

    public TitledPane inaccuracyPane;
    public LineChart<Double, Double> inaccuracyLineChart;

    public SplitPane vesSplitPane;
    public TitledPane vesPane;
    public LineChart<Double, Double> vesLineChart;
    public NumberAxis vesLineChartYAxis;
    public NumberAxis vesLineChartXAxis;

    public TableView<TableLine> experimentalTable;
    public TableColumn<TableLine, Double> experimentalAB_2Column;
    public TableColumn<TableLine, Double> experimentalResistanceApparentColumn;
    public TableColumn<TableLine, Double> experimentalErrorResistanceApparentColumn;

    @FXML
    public void onMenuFileOpenEXP() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл полевых данных для интерпретации");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("EXP - Полевые данные", "*.EXP", "*.exp")
        );
        File file = chooser.showOpenDialog(mainPane.getScene().getWindow());
//      если закрыть окно выбора файла, ничего не выбрав, то FileChooser вернет null
        if (file == null) {
            return;
        }

        Path openedFilePath = file.toPath();

        EXPFile openedEXP;
        try {
            openedEXP = SonetImport.readEXP(file);
        } catch (FileNotFoundException e) {
            alertWindowFileNotFoundException(e);
            return;
        }

        STTFile openedSTT;
        try {
            openedSTT = SonetImport.readSTT(new File(
                    openedFilePath.getParent().toString()
                            + File.separator
                            + openedEXP.getSTTFileName()));
        } catch (FileNotFoundException e) {
            alertWindowFileNotFoundException(e);
            return;
        }


        picket = new Picket(openedEXP, openedSTT);
        if (picket.getExperimentalData().isUnsafe()) {
            alertExperimentalDataIsUnsafe();
        }

        vesCurve = new VESCurve(vesLineChart, picket);
        vesCurve.createExperimentalCurve();
        ExperimentalTable.initializeWithData(
                experimentalTable,
                experimentalAB_2Column,
                experimentalResistanceApparentColumn,
                experimentalErrorResistanceApparentColumn,
                picket.getExperimentalData());


        inaccuracyLineChart.getData().clear();
        App.primaryStage.setTitle(file.getName() + " - GEM");
        menuFileOpenMOD.setDisable(false);
    }

    private void alertExperimentalDataIsUnsafe() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Режим совместимости");
        alert.setHeaderText("STT и EXP содержат разное количество строк");
        alert.setContentText("Будет отображаться минимально возможное число данных");
        alert.show();
    }

    private void alertWindowFileNotFoundException(FileNotFoundException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Файл не найден!");
        alert.setContentText(e.getMessage());
        alert.show();
        return;
    }

    @FXML
    public void onMenuFileOpenMOD() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл модели");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MOD - Данные модели", "*.MOD", "*.mod")
        );
        File file = chooser.showOpenDialog(mainPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        MODFile openedMOD;
        try {
            openedMOD = SonetImport.readMOD(file);
        } catch (FileNotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Файл не найден!");
            alert.setContentText(e.getMessage());
            alert.show();
            return;
        }

        picket.setModelData(new ModelData(openedMOD));
        try {
            vesCurve.createTheoreticalCurve();
            vesCurve.createModelCurve();
            InaccuracyCurve.initializeWithData(inaccuracyLineChart, inaccuracyPane, picket.getExperimentalData(), picket.getModelData());
        } catch (UnsatisfiedLinkError e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Невозможно решить прямую задачу");
            alert.setHeaderText("Отсутствует библиотека ForwardSolver");
            alert.setContentText(e.getMessage());
            alert.show();
            return;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            menuBar.setUseSystemMenuBar(true);
        }
        menuFileOpenEXP.setAccelerator(new KeyCodeCombination(
                KeyCode.O,
                KeyCombination.SHORTCUT_DOWN
        ));
        menuFileOpenMOD.setAccelerator(new KeyCodeCombination(
                KeyCode.O,
                KeyCombination.SHORTCUT_DOWN,
                KeyCombination.SHIFT_DOWN
        ));

        inaccuracyLineChart.setVisible(false);

        vesLineChart.setVisible(false);
        vesLineChartYAxis.setTickLabelFormatter(new PowerOf10Formatter());
        vesLineChartXAxis.setTickLabelFormatter(new PowerOf10Formatter());
    }
}
