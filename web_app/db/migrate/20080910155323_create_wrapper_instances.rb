class CreateWrapperInstances < ActiveRecord::Migration
  def self.up
    create_table :wrapper_instances do |t|
      t.string  :name
      t.text    :description
      t.integer :wrapper_id
      t.integer :source_id
      t.timestamps
    end

    add_index :wrapper_instances, [:name], :unique

  end

  def self.down
    drop_table :wrapper_instances
  end
end
