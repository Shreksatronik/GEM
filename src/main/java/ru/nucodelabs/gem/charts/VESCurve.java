package ru.nucodelabs.gem.charts;

import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import ru.nucodelabs.algorithms.ForwardSolver;
import ru.nucodelabs.data.Picket;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.log10;
import static java.lang.Math.max;

/**
 * <code>VESCurve</code> object is drawing VES Curves - experimental, theoretical and model on <code>LineChart</code>
 */
public class VESCurve {
    protected static final int EXP_CURVE_SERIES_CNT = 3;
    protected static final int THEOR_CURVE_SERIES_CNT = 4;
    protected static final int MOD_CURVE_SERIES_CNT = 5;
    protected static final int EXP_CURVE_SERIES_INDEX = 0;
    protected static final int EXP_CURVE_ERROR_UPPER_SERIES_INDEX = 1;
    protected static final int EXP_CURVE_ERROR_LOWER_SERIES_INDEX = 2;
    protected static final int THEOR_CURVE_SERIES_INDEX = THEOR_CURVE_SERIES_CNT - 1;
    protected static final int MOD_CURVE_SERIES_INDEX = MOD_CURVE_SERIES_CNT - 1;

    protected static final double EPSILON = 1e-6;

    private final LineChart<Double, Double> vesCurveLineChart;
    private final TitledPane vesCurvePane;
    private final Picket picket;

    /**
     * <code>VESCurve</code> object is drawing VES Curves - experimental, theoretical and model on <code>LineChart vesCurveLineChart</code>
     *
     * @param vesCurveLineChart Line Chart to draw curves
     * @param picket            picket
     */
    public VESCurve(LineChart<Double, Double> vesCurveLineChart, TitledPane vesCurvePane, Picket picket) {
        this.vesCurveLineChart = vesCurveLineChart;
        this.vesCurvePane = vesCurvePane;
        this.picket = picket;
    }

    /**
     * Creates experimental curve or overwriting existing one with upper and lower bounds according to error of measure.
     */
    public void createExperimentalCurve() {
        XYChart.Series<Double, Double> experimentalCurveSeries = getExperimentalCurveSeries();
        XYChart.Series<Double, Double> errorExperimentalCurveUpperBoundSeries = getErrorExperimentalCurveUpperBoundSeries();
        XYChart.Series<Double, Double> errorExperimentalCurveLowerBoundSeries = getErrorExperimentalCurveLowerBoundSeries();

        vesCurveLineChart.getData().clear();
        vesCurveLineChart.getXAxis().setAutoRanging(true);
        vesCurveLineChart.setVisible(true);
        vesCurveLineChart.getData().add(experimentalCurveSeries);
        vesCurveLineChart.getData().add(errorExperimentalCurveUpperBoundSeries);
        vesCurveLineChart.getData().add(errorExperimentalCurveLowerBoundSeries);
    }

    private XYChart.Series<Double, Double> getErrorExperimentalCurveLowerBoundSeries() {
        XYChart.Series<Double, Double> errorExperimentalCurveLowerBoundSeries = new XYChart.Series<>();
        for (int i = 0; i < picket.getExperimentalData().getSize(); i++) {
            double dotX = log10(picket.getExperimentalData().getAB_2().get(i));
            double error = picket.getExperimentalData().getErrorResistanceApparent().get(i) / 100f;
            double dotY = max(
                    log10(
                            picket.getExperimentalData().getResistanceApparent().get(i)
                                    - picket.getExperimentalData().getResistanceApparent().get(i) * error
                    ),
                    0
            );

            errorExperimentalCurveLowerBoundSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }

        errorExperimentalCurveLowerBoundSeries.setName("Нижняя граница погрешности");
        return errorExperimentalCurveLowerBoundSeries;
    }

    private XYChart.Series<Double, Double> getErrorExperimentalCurveUpperBoundSeries() {
        XYChart.Series<Double, Double> errorExperimentalCurveUpperBoundSeries = new XYChart.Series<>();
        for (int i = 0; i < picket.getExperimentalData().getSize(); i++) {
            double dotX = log10(picket.getExperimentalData().getAB_2().get(i));
            double error = picket.getExperimentalData().getErrorResistanceApparent().get(i) / 100f;
            double dotY = max(
                    log10(
                            picket.getExperimentalData().getResistanceApparent().get(i)
                                    + picket.getExperimentalData().getResistanceApparent().get(i) * error
                    ),
                    0
            );

            errorExperimentalCurveUpperBoundSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }

        errorExperimentalCurveUpperBoundSeries.setName("Верхняя граница погрешности");
        return errorExperimentalCurveUpperBoundSeries;
    }

    private XYChart.Series<Double, Double> getExperimentalCurveSeries() {
        XYChart.Series<Double, Double> experimentalCurveSeries = new XYChart.Series<>();
        for (int i = 0; i < picket.getExperimentalData().getSize(); i++) {
            double dotX = log10(picket.getExperimentalData().getAB_2().get(i));
            double dotY = max(log10(picket.getExperimentalData().getResistanceApparent().get(i)), 0);

            experimentalCurveSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }

        experimentalCurveSeries.setName("Экспериментальная кривая");
        return experimentalCurveSeries;
    }

    /**
     * Creates theoretical curve or overwriting existing one. Should be called only when experimental curve is already created.
     */
    public void createTheoreticalCurve() {
        XYChart.Series<Double, Double> theoreticalCurveSeries = getTheoreticalCurveSeries();

        if (vesCurveLineChart.getData().size() > EXP_CURVE_SERIES_CNT) {
            vesCurveLineChart.setData(vesCurveLineChart.getData().stream().limit(EXP_CURVE_SERIES_CNT)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        }
        vesCurveLineChart.getData().add(theoreticalCurveSeries);
    }

    private XYChart.Series<Double, Double> getTheoreticalCurveSeries() {
        XYChart.Series<Double, Double> theoreticalCurveSeries = new XYChart.Series<>();
        ArrayList<Double> solvedResistance = new ArrayList<>(ForwardSolver.ves(
                picket.getModelData().getResistance(),
                picket.getModelData().getPower(),
                picket.getExperimentalData().getAB_2()
        ));

        for (int i = 0; i < picket.getExperimentalData().getAB_2().size(); i++) {
            double dotX = log10(picket.getExperimentalData().getAB_2().get(i));
            double dotY = max(log10(((List<Double>) solvedResistance).get(i)), 0);
            theoreticalCurveSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }

        theoreticalCurveSeries.setName("Теоретическая кривая");
        return theoreticalCurveSeries;
    }

    /**
     * Creates model curve or overwriting existing one. Should be called only when theoretical curve is already created.
     */
    public void createModelCurve() {
        XYChart.Series<Double, Double> modelCurveSeries = getModelCurveSeries();

        if (vesCurveLineChart.getData().size() > THEOR_CURVE_SERIES_CNT) {
            vesCurveLineChart.setData(vesCurveLineChart.getData().stream().limit(THEOR_CURVE_SERIES_CNT)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList)));
        }
        vesCurveLineChart.getXAxis().setAutoRanging(false);
        vesCurveLineChart.getData().add(modelCurveSeries);
        vesCurveLineChart.getData().get(MOD_CURVE_SERIES_INDEX).getNode().setCursor(Cursor.HAND);
        vesCurveLineChart.getData().get(MOD_CURVE_SERIES_INDEX).getNode().setOnMouseDragged(this::mouseDraggedHandler);
    }

    private void mouseDraggedHandler(MouseEvent mouseEvent) {
        vesCurveLineChart.setAnimated(false);
        Point2D pointInScene = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());

        Double mouseX = vesCurveLineChart.getXAxis().getValueForDisplay(
                vesCurveLineChart.getXAxis().sceneToLocal(pointInScene).getX()
        );
        Double mouseY = vesCurveLineChart.getYAxis().getValueForDisplay(
                vesCurveLineChart.getYAxis().sceneToLocal(pointInScene).getY()
        );

        var points = vesCurveLineChart.getData().get(MOD_CURVE_SERIES_INDEX).getData();
        var closestVertical = points.stream()
                .filter(p -> p.getXValue() > mouseX - 0.1 && p.getXValue() < mouseX + 0.1)
                .toList();

        if (closestVertical.size() == 2) {
            closestVertical.get(0).setXValue(mouseX);
            closestVertical.get(1).setXValue(mouseX);
        } else {
            XYChart.Data<Double, Double> closestLeft = null;
            XYChart.Data<Double, Double> closestRight = null;

            for (XYChart.Data<Double, Double> point : points) {
                if (point.getXValue() < mouseX) {
                    closestLeft = point;
                }
                if (point.getXValue() > mouseX) {
                    closestRight = point;
                    break;
                }
            }

            if (closestLeft != null && closestRight != null) {
                closestLeft.setYValue(mouseY);
                closestRight.setYValue(mouseY);
            }
        }

        vesCurveLineChart.setAnimated(true);
    }

    private XYChart.Series<Double, Double> getModelCurveSeries() {
        XYChart.Series<Double, Double> modelCurveSeries = new XYChart.Series<>();

//        first point
        modelCurveSeries.getData().add(
                new XYChart.Data<>(
                        log10(0 + EPSILON),
                        log10(picket.getModelData().getResistance().get(0))
                )
        );

        Double prevSum = 0d;
        for (int i = 0; i < picket.getModelData().getResistance().size() - 1; i++) {
            final Double currentResistance = picket.getModelData().getResistance().get(i);
            final Double currentPower = new ArrayList<>(picket.getModelData().getPower()).get(i);

            modelCurveSeries.getData().add(
                    new XYChart.Data<>(
                            log10(currentPower + prevSum),
                            log10(currentResistance)
                    )
            );

            Double nextResistance = picket.getModelData().getResistance().get(i + 1);
            modelCurveSeries.getData().add(
                    new XYChart.Data<>(
                            log10(currentPower + prevSum),
                            log10(nextResistance)
                    )
            );
            prevSum += currentPower;
        }

//        last point
        final int lastResistanceIndex = picket.getModelData().getResistance().size() - 1;
        modelCurveSeries.getData().add(
                new XYChart.Data<>(
                        log10(max(
                                prevSum,
                                picket.getExperimentalData().getAB_2().get(picket.getExperimentalData().getSize() - 1)
                        )),
                        log10(picket.getModelData().getResistance().get(lastResistanceIndex))
                )
        );

        modelCurveSeries.setName("Кривая модели");
        return modelCurveSeries;
    }

    public Picket getPicket() {
        return picket;
    }
}
