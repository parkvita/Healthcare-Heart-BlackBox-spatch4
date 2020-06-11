package com.NSL.ECGCertification;

import java.util.ArrayList;

public class CalibList<T> extends ArrayList {
    private boolean isFinished;
    public void setCalibrationFinsish(boolean b)
    {
        isFinished = b;
    }
    public boolean isCalibrationFinished()
    {
        return isFinished;
    }
}
