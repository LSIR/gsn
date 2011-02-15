package gsn.utils.models;

public class ModelFitting {
	final public static int CONSTANT = 0;
	final public static int LINEAR = 1;
	final public static int QUADRATIC = 2;
	final public static int CHEBYSHEV_DEG1 = 3;
	final public static int CHEBYSHEV_DEG2 = 4;
	final public static int CHEBYSHEV_DEG3 = 5;
    final public static String MODEL_NAMES[] = {"constant",
                                                "linear",
                                                "quadratic",
                                                "chebyschev_deg1",
                                                "chebyschev_deg2",
                                                "chebyschev_deg3"
                                               };

	public static boolean FitAndMarkDirty(int model, double errorBound, int windowSize, double[] stream, long[] timestamps, double[] processed, double[] dirtyness)
	{
        /*
        //TODO: debug only
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < timestamps.length; i++) {
            sb.append(timestamps[i])
                    .append(", ")
                    .append(stream[i])
                    .append(", ")
                    .append(processed[i])
                    .append(", ")
                    .append(dirtyness[i])
                    .append("\n")
                    ;
        }

        System.out.println("before\n"+sb);
        //TODO: debug only
        */

        long[] _timestamps = new long[timestamps.length];
        //decode model
        for(int i = 0; i < timestamps.length; i++)
        {
        	_timestamps[i] -= timestamps[i]-timestamps[0];
        }

		//could fail
		IModel m;

		switch(model)
		{
			case CONSTANT:
				m = new ChebyshevPolynomialModel(0, windowSize, errorBound, _timestamps, stream);
				break;
			case LINEAR:
				m = new PolynomialModel(1, windowSize, errorBound, _timestamps, stream);
				break;
			case QUADRATIC:
				m = new PolynomialModel(2, windowSize, errorBound, _timestamps, stream);
				break;
			case CHEBYSHEV_DEG1:
				m = new ChebyshevPolynomialModel(1, windowSize, errorBound, _timestamps, stream);
				break;
			case CHEBYSHEV_DEG2:
				m = new ChebyshevPolynomialModel(2, windowSize, errorBound, _timestamps, stream);
				break;
			case CHEBYSHEV_DEG3:
				m = new ChebyshevPolynomialModel(3, windowSize, errorBound, _timestamps, stream);
				break;
			default:
				return false;
		}

		//fit
        boolean result = m.FitAndMarkDirty(processed, dirtyness);

        /*
        //TODO: debug only
         sb = new StringBuilder();

        for (int i = 0; i < timestamps.length; i++) {
            sb.append(timestamps[i])
                    .append(", ")
                    .append(stream[i])
                    .append(", ")
                    .append(processed[i])
                    .append(", ")
                    .append(dirtyness[i])
                    .append("\n")
                    ;
        }

        System.out.println("after\n"+sb);
        //TODO: debug only
        */

		return result;
	}



	public static void main(String[] argv)
    {
        double[] stream = new double[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 256, 128, 64, 32, 16, 8, 4, 2,  1, 1};

        long[] timestamps = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

        double[] processed = new double[20];
        double[] dirtyness = new double[20];

        //FitAndMarkDirty(CHEBYSHEV_DEG2, 0.5, 3, stream, timestamps, processed, dirtyness);
        FitAndMarkDirty(QUADRATIC, 5, 20, stream, timestamps, processed, dirtyness);

        for(int i = 0; i < processed.length; i++)
        {
            System.out.println(i+" : "+stream[i]+" => "+processed[i] + "("+dirtyness[i]+")");
        }
    }
}
