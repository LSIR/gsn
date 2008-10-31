class CreatePropertyValues < ActiveRecord::Migration
  def self.up
    create_table :property_values do |t|
      t.string :value
      t.references :prop_value_owner, :polymorphic => true
      t.belongs_to :property
      t.timestamps
    end
  end

  def self.down
    drop_table :property_values
  end
end
