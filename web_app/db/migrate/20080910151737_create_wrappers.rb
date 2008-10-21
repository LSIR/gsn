class CreateWrappers < ActiveRecord::Migration
  def self.up
    create_table :wrappers do |t|
      t.string :name
      t.string :description
      t.string :class_name

      t.timestamps
    end

    add_index :wrappers, [:name], :unique

  end

  def self.down
    drop_table :wrappers
  end
end
