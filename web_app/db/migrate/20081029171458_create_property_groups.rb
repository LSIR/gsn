class CreatePropertyGroups < ActiveRecord::Migration
  def self.up
    create_table :property_groups do |t|
      t.string :name
      t.string :description
      t.timestamps
    end
    
    add_index :property_groups, [:name], :unique
  end

  def self.down
    drop_table :property_groups
  end
end
