class CreateVirtualSensors < ActiveRecord::Migration
  def self.up
    create_table :virtual_sensors do |t|
      t.string :name
      t.integer :priority
      t.boolean :protected
      t.integer :pool_size
      t.string :storage_size
      t.boolean :unique_timestamp
      t.float :load_shedding
      t.integer :pc_instance_id
      t.integer :deployment_id
      t.timestamps
    end

    add_index :virtual_sensors, [:name], :unique

  end

  def self.down
    drop_table :virtual_sensors
  end
end
