class CreateWrapperParameters < ActiveRecord::Migration
  def self.up
    create_table :wrapper_parameters do |t|
      t.integer :wrapper_init_id
      t.integer :wrapper_instance_id
      t.string :value
      t.string :note

      t.timestamps
    end
  end

  def self.down
    drop_table :wrapper_parameters
  end
end
