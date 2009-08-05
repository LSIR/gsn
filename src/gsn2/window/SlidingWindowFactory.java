package gsn2.window;

import gsn2.sliding.CountBasedSliding;
import gsn2.sliding.LocalTimeBaseSliding;
import gsn2.sliding.SlidingHandler;

public class SlidingWindowFactory {
	
	public static SlidingHandler getSliding(String size) {
		int slidingValue = parseStringSize(size);
		if (isTimeBased(size))
			return new LocalTimeBaseSliding( slidingValue);
		else 
			return new CountBasedSliding(slidingValue);
	}

	public static WindowHandler getWindow(String size) {
		int windowSize = parseStringSize(size);
		if (isTimeBased(size))
			return new TimeBasedWindow(windowSize);
		else {
			if (windowSize<0)
				return new UnboundedWindow();
			else
			return new CountBasedWindow(windowSize);
		}
	}
		
	public static int parseStringSize(String size) {
		size = size.replace( " " , "" ).trim( ).toLowerCase( );
		if ( size.equalsIgnoreCase( "" ) ) 
			return 0;
		final int second = 1000;
		final int minute = second * 60;
		final int hour = minute * 60;
		final int mIndex = size.indexOf( "m" );
		final int hIndex = size.indexOf( "h" );
		final int sIndex = size.indexOf( "s" );
		if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) 
			return Integer.parseInt( size );
		final StringBuilder shs = new StringBuilder( size);
		if ( mIndex >= 0 && mIndex == shs.length() - 1) 
			return Integer.parseInt( shs.deleteCharAt( mIndex ).toString( ) ) * minute;
		else if ( hIndex >= 0 && hIndex == shs.length() - 1) 
			return Integer.parseInt( shs.deleteCharAt( hIndex ).toString( ) ) * hour;
		else if ( sIndex >= 0 && sIndex == shs.length() - 1) 
			return Integer.parseInt( shs.deleteCharAt( sIndex ).toString( ) ) * second;
		return 0;
	}

	public static boolean isTimeBased(String size) {
		size = size.replace( " " , "" ).trim( ).toLowerCase( );
		final int mIndex = size.indexOf( "m" );
		final int hIndex = size.indexOf( "h" );
		final int sIndex = size.indexOf( "s" );
		if ( mIndex < 0 && hIndex < 0 && sIndex < 0 ) 
			return false;
		return true;
	}

}
