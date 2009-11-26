#!/usr/bin/ruby1.8
require 'csv'

# This script generates a CSV file that can be imported in your DB.
# It reads a CSV file, slect the desired columns and convert the NULL values.
# The size of the files generated can be limited to a number of lines.
#
# Usage: ruby csv-data-formatter.rb <DATA_FILE> <TABLE_NAME> <COLUMN_SEPARATOR> <COLUMN_INDEXES> <NULL_REPRESENTATIONS>
#
# <DATA_FILE>				The path to the csv data file.
# <TABLE_NAME>				The database table name. This should be equal to the virtual sensor name
# <COLUMNS_SEPARATOR> 		The csv colums separator in the data file. 
#                           You can add a process on the value separated by a ':' eg. 1,4:*100,3,5 -> The elements from the column 4 will be multiplied by 100.
#                           You can add empty columns with the letter E eg. 1,E,4,6 -> The second column in the output data file will be empty
# <COLUMN_INDEXES>			An ordered list (comma separated) of the column indexes to select from the data file.
# <NULL_REPRESENTATIONS>	A case sensitive list (comma separated) of string that would be converted to NULL if found in the data file
#                           eg. nan,NaN,NAN
#
# Record station 2 eg. ruby csv-data-formatter.rb thur-meteo-2.dat record_sensorscope_2 ' ' E,7*1000,0,7*1000,8,11,9,23,24,25,17,18,19,20,21,22 "NaN"

#################################
# Import into db
#################################
# mysqlimport     --host=localhost \
#                --user=root \
#                --password \
#                --fields-terminated-by=',' \
#                --debug \
#                --local \
#                --lock-tables \
#                --fields-optionally-enclosed-by='"' \
#                gsn ./record_sensorscope_2.csv
#################################


# Get the script parameters
SOURCE = ARGV[0]
TABLE_NAME = ARGV[1]
COLUMN_SEPARATOR = ARGV[2]
COLUMN_INDEXES = ARGV[3].split(',')
NULL_REPRESENTATIONS = ARGV[4].split(',')

# Process the data file

reader = CSV.open(SOURCE, 'r', COLUMN_SEPARATOR)
CSV.open("#{TABLE_NAME}.csv", 'wb') do |writer|
	reader.each_with_index do |row, i|
		output_row = []
		COLUMN_INDEXES.each do |index|
			if index.eql? 'E'
				output_row << [ '' ]
			else 
				i = index.split(':')
				# Change representation to NILL if the value match one of the NULL Representation
				is_null = NULL_REPRESENTATIONS.inject(false){ |match,guess| match || (row[i[0].to_i].eql? guess) }
				if is_null
					output_row << [ 'NULL' ]
				else
					next_elt = i[1].nil? ? row[i[0].to_i] : (eval"#{row[i[0].to_i]} #{i[1]}")
					output_row << [ next_elt ]
				end
			end
		end
		writer << output_row
	end
end
