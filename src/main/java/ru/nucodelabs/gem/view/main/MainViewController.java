package ru.nucodelabs.gem.view.main;

import io.reactivex.rxjava3.subjects.Subject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;
import org.apache.commons.math3.analysis.MultivariateFunction;
import ru.nucodelabs.algorithms.inverse_solver.InverseSolver;
import ru.nucodelabs.algorithms.inverse_solver.inverse_functions.FunctionValue;
import ru.nucodelabs.algorithms.inverse_solver.inverse_functions.SquaresDiff;
import ru.nucodelabs.data.ves.ExperimentalData;
import ru.nucodelabs.gem.core.events.PicketSwitchEvent;
import ru.nucodelabs.gem.core.events.SectionChangeEvent;
import ru.nucodelabs.gem.core.events.ViewEvent;
import ru.nucodelabs.gem.core.utils.OSDetect;
import ru.nucodelabs.gem.model.Section;
import ru.nucodelabs.gem.view.AbstractSectionController;
import ru.nucodelabs.gem.view.alerts.ExceptionAlert;
import ru.nucodelabs.gem.view.alerts.IncorrectFileAlert;
import ru.nucodelabs.gem.view.alerts.UnsafeDataAlert;
import ru.nucodelabs.gem.view.filechoosers.EXPFileChooserFactory;
import ru.nucodelabs.gem.view.filechoosers.JsonFileChooserFactory;
import ru.nucodelabs.gem.view.filechoosers.MODFileChooserFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

public class MainViewController extends AbstractSectionController {

    private ResourceBundle uiProperties;

    /**
     * Properties
     */
    private final StringProperty vesTitle;
    private final StringProperty vesNumber;
    private final BooleanProperty noFileOpened;

    @Inject
    public MainViewController(Subject<ViewEvent> viewEvents, Section section) {
        super(viewEvents, section);

        currentPicket = -1;
        noFileOpened = new SimpleBooleanProperty(true);
        vesTitle = new SimpleStringProperty("");

        vesNumber = new SimpleStringProperty("0/0");
    }

    @FXML
    public Stage root;
    @FXML
    public MenuBar menuBar;
    @FXML
    public Menu menuView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        uiProperties = requireNonNull(resources);

        if (OSDetect.isMacOS()) {
            CheckMenuItem useSystemMenu = new CheckMenuItem(uiProperties.getString("useSystemMenu"));
            menuView.getItems().add(0, useSystemMenu);
            useSystemMenu.selectedProperty().bindBidirectional(menuBar.useSystemMenuBarProperty());
        }
    }

    @Override
    protected Stage getStage() {
        return root;
    }

    @FXML
    public void closeFile() {
        getStage().close();
    }

    /**
     * Asks which EXP files and then imports them to current window
     */
    @FXML
    public void importEXP() {
        List<File> files = new EXPFileChooserFactory().create().showOpenMultipleDialog(getStage());
        if (files != null && files.size() != 0) {
            for (var file : files) {
                addEXP(file);
            }
        }
    }

    @FXML
    public void openSection() {
        File file = new JsonFileChooserFactory().create().showOpenDialog(getStage());
        if (file != null) {
            try {
                section.loadFromJson(file);
            } catch (Exception e) {
                new IncorrectFileAlert(e, getStage()).show();
                return;
            }
            viewEvents.onNext(new PicketSwitchEvent(0));
            viewEvents.onNext(new SectionChangeEvent());
        }
    }

    @FXML
    public void saveSection() {
        File file = new JsonFileChooserFactory().create().showSaveDialog(getStage());
        if (file != null) {
            try {
                section.saveToJson(file);
            } catch (Exception e) {
                new IncorrectFileAlert(e, getStage()).show();
            }
        }
    }

    /**
     * Opens new window
     */
    @FXML
    public void newWindow() {
        try {
            new MainViewFactory().create().show();
        } catch (IOException e) {
            new ExceptionAlert(e).show();
        }
    }

    /**
     * Asks which file to import and then import it
     */
    @FXML
    public void importMOD() {
        File file = new MODFileChooserFactory().create().showOpenDialog(getStage());

        if (file == null) {
            return;
        }

        try {
            section.loadModelDataFromMODFile(currentPicket, file);
        } catch (Exception e) {
            new IncorrectFileAlert(e, getStage()).show();
        }
        viewEvents.onNext(new SectionChangeEvent());
    }

    @FXML
    public void switchToNextPicket() {
        if (section.getPicketsCount() > currentPicket + 1) {
            viewEvents.onNext(new PicketSwitchEvent(currentPicket + 1));
        }
    }

    @FXML
    public void switchToPrevPicket() {
        if (currentPicket > 0 && section.getPicketsCount() > 0) {
            viewEvents.onNext(new PicketSwitchEvent(currentPicket - 1));
        }
    }

    @FXML
    public void inverseSolve() {
        //Размер симплекса (по каждому измерению)
        final double SIDE_LENGTH = 0.1;

        //Какие-то константы для SimplexOptimize
        final double RELATIVE_THRESHOLD = 1e-10;
        double ABSOLUTE_THRESHOLD = 1e-30;

        MultivariateFunction multivariateFunction = new FunctionValue(
                section.getPicket(currentPicket).experimentalData(),
                new SquaresDiff());

        InverseSolver inverseSolver = new InverseSolver(
                section.getPicket(currentPicket),
                SIDE_LENGTH, RELATIVE_THRESHOLD,
                ABSOLUTE_THRESHOLD,
                multivariateFunction);

        try {
            section.setModelData(currentPicket,
                    inverseSolver.getOptimizedPicket());
            viewEvents.onNext(new SectionChangeEvent());
        } catch (Exception e) {
            new UnsafeDataAlert(section.getName(currentPicket), getStage()).show();
        }
    }

    private void addEXP(File file) {
        try {
            section.loadExperimentalDataFromEXPFile(currentPicket + 1, file);
        } catch (Exception e) {
            new IncorrectFileAlert(e, getStage()).show();
            return;
        }
        viewEvents.onNext(new PicketSwitchEvent(currentPicket + 1));
        viewEvents.onNext(new SectionChangeEvent());
        compatibilityModeAlert();
    }

    /**
     * Adds files names to vesText
     */
    private void updateVESText() {
        vesTitle.set(section.getName(currentPicket));
    }

    private void updateVESNumber() {
        vesNumber.set(currentPicket + 1 + "/" + section.getPicketsCount());
    }

    /**
     * Warns about compatibility mode if data is unsafe
     */
    private void compatibilityModeAlert() {
        ExperimentalData experimentalData = section.getPicket(currentPicket).experimentalData();
        if (experimentalData.isUnsafe()) {
            new UnsafeDataAlert(section.getName(currentPicket), getStage()).show();
        }
    }

    @Override
    protected void update() {
        if (section.getPicketsCount() > 0) {
            noFileOpened.set(false);
        }
        updateVESText();
        updateVESNumber();
    }

    public String getVesTitle() {
        return vesTitle.get();
    }

    public StringProperty vesTitleProperty() {
        return vesTitle;
    }

    public boolean getNoFileOpened() {
        return noFileOpened.get();
    }

    public BooleanProperty noFileOpenedProperty() {
        return noFileOpened;
    }

    public String getVesNumber() {
        return vesNumber.get();
    }

    public StringProperty vesNumberProperty() {
        return vesNumber;
    }
}
