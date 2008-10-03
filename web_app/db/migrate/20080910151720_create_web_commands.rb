class CreateWebCommands < ActiveRecord::Migration
  def self.up
    create_table :web_commands do |t|
      t.string :name
      t.string :description
      t.string :rule
      t.integer :web_input_id
      t.timestamps
    end
  end

  def self.down
    drop_table :web_commands
  end
end
