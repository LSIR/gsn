#!/usr/bin/ruby
require 'rubygems'
require 'tenjin'

# This script generates the CSV virtual sensor description file. The output is available in the output.xml file
#
# Usage: ruby tramm-vs-generator.rb <DATA_PATH> <FORMAT_PATH> <SKIP_LINES> <SEPARATOR> <LINE_NAMES> <LINES_DESCRIPTION> <LINE_TYPES>
# eg.  : ruby tramm-vs-generator.rb /home/marett/tramm/CR1000_SMALLREL_RADIO_TDR_Wave.dat /home/marett/tramm/CR1000_SMALLREL_RADIO_TDR_Wave.dat 5 , 2 3,4 5
#
# Note: '(' caracters in name fields are replaced with a '_' and ')' are removed
#
# <DATA_PATH>         : The ABSOLUTE path to the file that contains the CSV data
# <FORMAT_PATH>       : The ABSOLUTE path to the file that contains the 'data format' 
# <SKIP_LINES>        : The number of lines to skip in the beginning of the data file
# <SEPARATOR>         : The field separator in the data file
# <LINE_NAMES>        : The line in the format file that contains the field names
# <LINES_DESCRIPTION> : The lines in the format file that contains the field descriptions, possibly many separated by a comma
# <LINE_TYPES>        : The line in the format file that contains the field types


# Get the script parameters

DATA_PATH = ARGV[0]
FORMAT_PATH = ARGV[1]
SKIP_LINES = ARGV[2]
SEPARATOR = ARGV[3]
LINE_NAMES = ARGV[4]
LINES_DESCRIPTION = ARGV[5]
LINE_TYPES = ARGV[6]

# Get the fields descriptions

FIELDS , TYPES = [] , []
lines = Array.new
File.open(FORMAT_PATH,"r") { |file| file.each_line {|line| lines << line} }
lines[1].split(",").each_with_index{|item,index| FIELDS << item.strip.upcase.sub(/[(]/, '_').delete(")").sub(/^"/, '').sub(/"$/, '') } # Get the fields names
lines[4].split(",").each_with_index{|item,index| TYPES << item.strip.upcase.sub(/^"/, '').sub(/"$/, '') } # Get the type field

# Change the Time format to BIGINT
TYPES.each_with_index { |line, index|
	if /^(TS:)/.match(line) then 
		TYPES[index] = "BIGINT"
		puts TYPES[index]
	end
}

# Generate the output XML file

engine = Tenjin::Engine.new()
context = {
	:items=>FIELDS, 
	:types=>TYPES,
	:dataPath=>DATA_PATH,
	:formatPath=>FORMAT_PATH,
	:skipLines=>SKIP_LINES,
	:separator=>SEPARATOR,
	:lineNames=>LINE_NAMES,
	:linesDescription=>LINES_DESCRIPTION,
	:lineTypes=>LINE_TYPES
}
output = engine.render('template.rbxml', context)
print output
File.open("output.xml",'w+'){|f| f<<output }

 


