class CreateUnits < ActiveRecord::Migration
  def self.up
    create_table :units do |t|
      t.string :name
      t.string :description
      t.timestamps
    end
    
    add_index :units, [:name], :unique

  end

  def self.down
    drop_table :units
  end
end
