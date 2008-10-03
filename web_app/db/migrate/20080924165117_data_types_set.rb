class DataTypesSet < ActiveRecord::Migration
  def self.up
    data_type = DataType.new({:name => 'VARCHAR', :description => 'String of Characters'}).save
    data_type = DataType.new({:name => 'CHAR', :description => 'Single Character'}).save
    data_type = DataType.new({:name => 'BIGINT', :description => 'Signed 64 bits integer'}).save
    data_type = DataType.new({:name => 'INTEGER', :description => 'Signed 32 bits integer'}).save
    data_type = DataType.new({:name => 'SMALLINT', :description => 'Signed 16 bits integer'}).save
    data_type = DataType.new({:name => 'TINYINT', :description => 'Signed 8 bits integer'}).save
    data_type = DataType.new({:name => 'DOUBLE', :description => 'Signed 64 bits float'}).save
    data_type = DataType.new({:name => 'BINARY', :description => 'Unformatted binary'}).save
  end

  def self.down
    DataType.delete_all
  end
end
