package ru.nucodelabs.gem;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import ru.nucodelabs.files.sonet.EXPFile;
import ru.nucodelabs.files.sonet.STTFile;

import static java.lang.Math.log10;

public class ExperimentalCurve {

    protected static void makeCurve(LineChart<Double, Double> vesCurve, STTFile openedSTT, EXPFile openedEXP) {
        vesCurve.getData().clear();
        vesCurve.getData().add(makeCurveData(openedSTT, openedEXP));
        vesCurve.getData().add(makeCurveErrorUpper(openedSTT, openedEXP));
        vesCurve.getData().add(makeCurveErrorLower(openedSTT, openedEXP));
    }

    protected static XYChart.Series<Double, Double> makeCurveData(STTFile openedSTT, EXPFile openedEXP) {
        XYChart.Series<Double, Double> pointsSeries = new XYChart.Series<>();

        for (int i = 0; i < openedSTT.getAB_2().size(); i++) {
            double dotX = log10(openedSTT.getAB_2().get(i));
            double dotY = log10(openedEXP.getResistanceApp().get(i));
            pointsSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }
        return pointsSeries;
    }

    protected static XYChart.Series<Double, Double> makeCurveErrorUpper(STTFile openedSTT, EXPFile openedEXP) {
        XYChart.Series<Double, Double> pointsSeries = new XYChart.Series<>();

        for (int i = 0; i < openedSTT.getAB_2().size(); i++) {
            double dotX = log10(openedSTT.getAB_2().get(i));
            double resistance = openedEXP.getResistanceApp().get(i);
            double error = openedEXP.getErrorResistanceApp().get(i) / 100f;
            double dotY = log10(resistance + resistance * error);
            pointsSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }
        return pointsSeries;
    }

    protected static XYChart.Series<Double, Double> makeCurveErrorLower(STTFile openedSTT, EXPFile openedEXP) {
        XYChart.Series<Double, Double> pointsSeries = new XYChart.Series<>();

        for (int i = 0; i < openedSTT.getAB_2().size(); i++) {
            double dotX = log10(openedSTT.getAB_2().get(i));
            double resistance = openedEXP.getResistanceApp().get(i);
            double error = openedEXP.getErrorResistanceApp().get(i) / 100f;
            double dotY = log10(resistance - resistance * error);
            pointsSeries.getData().add(new XYChart.Data<>(dotX, dotY));
        }
        return pointsSeries;
    }
}
