package ru.nucodelabs.data.ves;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.DEDUCTION,
        defaultImpl = ExperimentalDataImpl.class
)
public interface ExperimentalData extends Serializable {

    static ExperimentalData create(
            double ab2,
            double mn2,
            double resistanceApparent,
            double errorResistanceApparent,
            double amperage,
            double voltage
    ) {
        return new ExperimentalDataImpl(
                ab2, mn2, resistanceApparent, errorResistanceApparent, amperage, voltage);
    }

    /**
     * AB/2, м
     */
    @Min(0) double getAb2();

    /**
     * MN/2, м
     */
    @Min(0) double getMn2();

    /**
     * Сопротивление кажущееся, Ом * м
     */
    @Min(0) double getResistanceApparent();

    /**
     * Погрешность, %
     */
    @Min(0) double getErrorResistanceApparent();

    /**
     * Ток, мА
     */
    @Min(0) double getAmperage();

    /**
     * Напряжение, мВ
     */
    @Min(0) double getVoltage();
}