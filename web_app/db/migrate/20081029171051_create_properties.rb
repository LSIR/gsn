class CreateProperties < ActiveRecord::Migration
  def self.up
    create_table :properties do |t|
      t.string :name
      t.string :description
      t.belongs_to :property_group
      t.references :unit
      t.timestamps
    end
  end

  def self.down
    drop_table :properties
  end
end
