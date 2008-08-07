$:.unshift File.join(File.dirname(__FILE__),'.')

require 'adapters/derby'
require 'adapters/h2'
require 'adapters/hsqldb'
require 'adapters/mysql'
require 'adapters/postgresql'

# A set of wrapper classes to encapsulate each database engine. Each adapter
# requires the standard connection parameters on instantiation. Two methods
# are expected to exist on each adapter: class_name and connection_string.
