package gsn.utils.models;

import gsn.utils.models.jgarch.armamodel.ARModel;
import gsn.utils.models.jgarch.garchmodel.GarchModel;
import gsn.utils.models.jgarch.util.ArrayUtils;
import gsn.utils.models.jgarch.wrappers.REngineManager;

import java.util.List;

public class ArmaGarchModel implements IModel {
    private double[] stream;

    private int windowSize;

    private double errorBound = 3;

    private double minVar = 1E-4;

    ArmaGarchModel(int windowSize, double errorBound, double[] stream) {
        this.stream = stream;
        this.windowSize = windowSize;
        this.errorBound = errorBound;
        //System.out.println(""+windowSize);
        //System.out.println(""+errorBound);

    }


    public boolean FitAndMarkDirty(double[] processed, double[] dirtyness) {
        //System.out.println("FitAndMarkDirty @ ARMA_GARCH");
        boolean allClean = true;
        //double [] predUVar = new double[stream.length+1];
        //double [] predLVar = new double[stream.length+1];
        //double [] predValue = new double[stream.length+1];

        // will them with NaNs until the windowSize is reached
        for (int i = 0; i < windowSize; i++) {
            //predUVar[i] = Double.NaN;
            //predLVar[i] = Double.NaN;
            //predValue[i] = Double.NaN;
            processed[i] = stream[i];
            dirtyness[i] = 0;
        }

        // Sliding Window
        double[] tseries = new double[windowSize];

        /*
        System.out.println("windowSIze => " +    windowSize);
        for (double t:tseries) {
				System.out.print(t+ ",");
			}
	    */

        int j = stream.length - windowSize - 1;
        //System.out.println("j => "+j + " stream.length "+ stream.length);

        for (int i = 0; i <= (stream.length - windowSize - 1); i++) {
            int currIdx = i + windowSize;

            //System.out.println("i => " + i);

            System.arraycopy(stream, i, tseries, 0, windowSize);

            for (double t : tseries) {
                System.out.print(t + ",");
            }

            System.out.println();
            System.out.println(i + windowSize);


            // create and execute AR model
            ARModel ar = new ARModel(tseries);
            ar.run();

            // predict next value from AR model
            double[] arPred = ar.getArPreds();
            double predValue = arPred[0];

            // Get residuals from AR model and give them to GARCH model
            double[] arResid = ar.getArResiduals();
            GarchModel gm = new GarchModel(arResid);
            gm.run();

            // Predict +ve and -ve variance from GARCH model.
            double predUVar = gm.getPredUVar();
            double predLVar = gm.getPredLVar();

            System.out.println(gm.getPredUVar());
            System.out.println(gm.getPredLVar());

            double quality = 0;
            if (predUVar != 0.0)
                quality = 1 - (stream[currIdx] - predValue) / (3 * predUVar);

            if (predUVar > minVar) {
                if ((stream[currIdx] <= predValue + errorBound * Math.sqrt(predUVar)) &&
                        (stream[currIdx] >= predValue + errorBound * Math.sqrt(predLVar))) {
                    processed[currIdx] = stream[currIdx];
                    dirtyness[currIdx] = 0;

                } else {
                    processed[currIdx] = predValue;
                    dirtyness[currIdx] = 1;
                    allClean = false;
                }

            } else {
                processed[currIdx] = stream[currIdx];
                dirtyness[currIdx] = 0;
            }
        }

        return allClean;
    }


}
