class CreateAddressings < ActiveRecord::Migration
  def self.up
    create_table :addressings do |t|
      t.string :value
      t.integer :addressing_type_id
      t.integer :virtual_sensor_id
      t.timestamps
    end
  end

  def self.down
    drop_table :addressings
  end
end
