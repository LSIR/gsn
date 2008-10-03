class CreateWrapperInstances < ActiveRecord::Migration
  def self.up
    create_table :wrapper_instances do |t|
      t.string  :name
      t.text    :description
      t.integer :wrapper_id
      t.integer :source_id

      t.timestamps
    end
  end

  def self.down
    drop_table :wrapper_instances
  end
end
