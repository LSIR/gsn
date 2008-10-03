class CreatePcParameters < ActiveRecord::Migration
  def self.up
    create_table :pc_parameters do |t|
      t.integer :pc_init_id
      t.integer :pc_instance_id
      t.string :value
      t.string :note

      t.timestamps
    end
  end

  def self.down
    drop_table :pc_parameters
  end
end
