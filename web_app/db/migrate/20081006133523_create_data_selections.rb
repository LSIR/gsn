class CreateDataSelections < ActiveRecord::Migration
  def self.up
    create_table :data_selections do |t|
      t.integer :data_configuration_id
      t.integer :output_format_id
      t.integer :virtual_sensor_id
      t.timestamps
    end
  end

  def self.down
    drop_table :data_selections
  end
end
