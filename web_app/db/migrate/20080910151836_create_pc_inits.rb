class CreatePcInits < ActiveRecord::Migration
  def self.up
    create_table :pc_inits do |t|
      t.string :name
      t.string :description
      t.string :default_value
      t.boolean :optional
      t.integer :data_type_id
      t.integer :processor_id
      t.timestamps
    end
  end

  def self.down
    drop_table :pc_inits
  end
end
