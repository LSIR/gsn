/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/beans/DataTypes.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.beans;
import java.util.regex.Pattern;
import tinygsn.utils.GSNRuntimeException;

public class DataTypes {

	public final static String OPTIONAL_NUMBER_PARAMETER = "\\s*(\\(\\s*\\d+\\s*\\))?";
	public final static String REQUIRED_NUMBER_PARAMETER = "\\s*\\(\\s*\\d+\\s*\\)";

	// NEXT FIELD
	public final static String VAR_CHAR_PATTERN_STRING = "\\s*varchar" + REQUIRED_NUMBER_PARAMETER + "\\s*";
	public final static byte VARCHAR = 0;
	public final static String VARCHAR_NAME = "Varchar(256)";

	// NEXT FIELD
	public final static String CHAR_PATTERN_STRING = "\\s*char" + REQUIRED_NUMBER_PARAMETER + "\\s*";
	public final static byte CHAR = 1;
	public final static String CHAR_NAME = "Char(256)";

	// NEXT FIELD
	public final static String INTEGER_PATTERN_STRING = "\\s*((INTEGER)|(INT))\\s*";
	public final static byte INTEGER = 2;
	public final static String INTEGER_NAME = "Integer";

	// NEXT FIELD
	public final static String BIGINT_PATTERN_STRING = "\\s*BIGINT\\s*";
	public final static byte BIGINT = 3;
	public final static String BIGINT_NAME = "BigInt";

	// NEXT FIELD
	public final static String BINARY_PATTERN_STRING = "\\s*(BINARY|BLOB)" + OPTIONAL_NUMBER_PARAMETER + "(\\s*:.*)?";
	public final static byte BINARY = 4;
	public final static String BINARY_NAME = "Binary";

	// NEXT FIELD
	public final static String DOUBLE_PATTERN_STRING = "\\s*DOUBLE\\s*";
	public final static byte DOUBLE = 5;
	public final static String DOUBLE_NAME = "Double";

	// NEXT FIELD
	/**
	 * Type Time is not supported at the moment. If you want to present time,
	 * please use longint. For more information consult the GSN mailing list on
	 * the same subject.
	 */
	public final static String TIME_PATTERN_STRING = "\\s*TIME\\s*";
	public final static byte TIME = 6;
	public final static String TIME_NAME = "Time";

	// NEXT FIELD
	public final static String TINYINT_PATTERN_STRING = "\\s*TINYINT\\s*";
	public final static byte TINYINT = 7;
	public final static String TINYINT_NAME = "TinyInt";

	// NEXT FIELD
	public final static String SMALLINT_PATTERN_STRING = "\\s*SMALLINT\\s*";
	public final static byte SMALLINT = 8;
	public final static String SMALLINT_NAME = "SmallInt";

	// NEXT FIELD
	public final static String FLOAT_PATTERN_STRING = "\\s*FLOAT\\s*";
	public final static byte FLOAT = 9;
	public final static String FLOAT_NAME = "Float";

	// FINISH
	public final static Pattern[] ALL_PATTERNS = new Pattern[] {
			Pattern.compile(VAR_CHAR_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(CHAR_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(INTEGER_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(BIGINT_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(BINARY_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(DOUBLE_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(TIME_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(TINYINT_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(SMALLINT_PATTERN_STRING, Pattern.CASE_INSENSITIVE),
			Pattern.compile(FLOAT_PATTERN_STRING, Pattern.CASE_INSENSITIVE) };

	public final static StringBuilder ERROR_MESSAGE = new StringBuilder(
			"Acceptable types are (TINYINT,SMALLINT,INTEGER,BIGINT,CHAR(#),BINARY[(#)],VARCHAR(#),DOUBLE,TIME,FLOAT).");

	public final static String[] TYPE_NAMES = new String[] { VARCHAR_NAME,
			CHAR_NAME, INTEGER_NAME, BIGINT_NAME, BINARY_NAME, DOUBLE_NAME,
			TIME_NAME, TINYINT_NAME, SMALLINT_NAME, FLOAT_NAME };

	public static byte convertTypeNameToGSNTypeID(final String type) {
		if (type == null)
			throw new GSNRuntimeException(new StringBuilder(
					"The type *null* is not recoginzed by GSN.").append(
					DataTypes.ERROR_MESSAGE).toString());
		if (type.trim().equalsIgnoreCase("string"))
			return DataTypes.VARCHAR;
		for (byte i = 0; i < DataTypes.ALL_PATTERNS.length; i++)
			if (DataTypes.ALL_PATTERNS[i].matcher(type).matches()) {
				return i;
			}
		if (type.trim().equalsIgnoreCase("numeric"))
			return DataTypes.DOUBLE;

		throw new GSNRuntimeException(new StringBuilder("The type *").append(type)
				.append("* is not recognized.").append(DataTypes.ERROR_MESSAGE)
				.toString());
	}

}
