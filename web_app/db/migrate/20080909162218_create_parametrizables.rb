class CreateParametrizables < ActiveRecord::Migration
  def self.up
    create_table :parametrizables do |t|
      t.string :name
      t.text :description
      t.string :type #required for single table inheritance
      t.integer :resource_id
    end
  end

  def self.down
    drop_table :parametrizables
  end
end
