package ch.ethz.permafrozer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

final class MsgFormatter
{
	final static DecimalFormat DECIMAL_1FRAC = new DecimalFormat("0.0");
	final static DecimalFormat DECIMAL_2FRAC = new DecimalFormat("0.00");
	final static SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
}
