class AddressingTypeSet < ActiveRecord::Migration
  def self.up
    AddressingType.new({:name => 'geographical', :description => 'Textual description of the location.'}).save
    AddressingType.new({:name => 'latitude', :description => 'WGS84 latitude.'}).save
    AddressingType.new({:name => 'longitude', :description => 'WGS84 longitude.'}).save
    AddressingType.new({:name => 'altitude', :description => 'WGS 84 altitude.'}).save
  end

  def self.down
    AddressingType.delete_all
  end
end
