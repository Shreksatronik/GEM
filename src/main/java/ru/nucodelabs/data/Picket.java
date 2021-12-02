package ru.nucodelabs.data;

import ru.nucodelabs.files.sonet.EXPFile;
import ru.nucodelabs.files.sonet.MODFile;
import ru.nucodelabs.files.sonet.STTFile;

public class Picket {
    private ExperimentalData experimentalData;
    private ModelData modelData;

    public Picket() {
        this.experimentalData = new ExperimentalData();
        this.modelData = new ModelData();
    }

    public Picket(EXPFile expFile, STTFile sttFile) {
        experimentalData = new ExperimentalData(expFile, sttFile);
    }

    public Picket(EXPFile expFile, STTFile sttFile, MODFile modFile) {
        this(expFile, sttFile);
        modelData = new ModelData(modFile);
    }

    public ExperimentalData getExperimentalData() {
        return experimentalData;
    }

    public void setExperimentalData(ExperimentalData experimentalData) {
        this.experimentalData = experimentalData;
    }

    public ModelData getModelData() {
        return modelData;
    }

    public void setModelData(ModelData modelData) {
        this.modelData = modelData;
    }
}
