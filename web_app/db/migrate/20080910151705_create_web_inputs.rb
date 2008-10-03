class CreateWebInputs < ActiveRecord::Migration
  def self.up
    create_table :web_inputs do |t|
      t.string :name
      t.string :description
      t.integer :processor_id
      t.timestamps
    end
  end

  def self.down
    drop_table :web_inputs
  end
end
