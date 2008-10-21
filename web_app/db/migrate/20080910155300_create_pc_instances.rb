class CreatePcInstances < ActiveRecord::Migration
  def self.up
    create_table :pc_instances do |t|
      t.string :name
      t.string :web_password
      t.integer :processor_id
      t.timestamps
    end

    add_index :pc_instances, [:name], :unique

  end

  def self.down
    drop_table :pc_instances
  end
end
