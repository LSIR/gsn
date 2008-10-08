class CreateCriterions < ActiveRecord::Migration
  def self.up
    create_table :criterions do |t|
      t.integer :data_selection_id
      t.boolean :not
      t.string :operator
      t.string :join
      t.string :value
      t.timestamps
    end
  end

  def self.down
    drop_table :criterions
  end
end
