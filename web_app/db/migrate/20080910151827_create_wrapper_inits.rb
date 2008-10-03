class CreateWrapperInits < ActiveRecord::Migration
  def self.up
    create_table :wrapper_inits do |t|
      t.string :name
      t.string :description
      t.string :default_value
      t.boolean :optional
      t.integer :data_type_id
      t.integer :wrapper_id
      t.timestamps
    end
  end

  def self.down
    drop_table :wrapper_inits
  end
end
