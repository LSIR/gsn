$:.unshift File.join(File.dirname(__FILE__),'.')

require 'java'

# Wraps the java.sql package in 
# a module and makes it available
# to the JDBC module.
module JavaSql
  include_package 'java.sql'
end

require 'jdbc/db'
require 'jdbc/result'
require 'jdbc/prepared_statement'
