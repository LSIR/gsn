class CreateAddressingTypes < ActiveRecord::Migration
  def self.up
    create_table :addressing_types do |t|
      t.string :name
      t.string :description
      t.timestamps
    end

    add_index :addressing_types, [:name], :unique

  end

  def self.down
    drop_table :addressing_types
  end
end
