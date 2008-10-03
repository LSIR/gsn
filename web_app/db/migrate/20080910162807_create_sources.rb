class CreateSources < ActiveRecord::Migration
  def self.up
    create_table :sources do |t|
      t.string :name
      t.string :window_size
      t.string :sliding
      t.float :load_shedding
      t.text :query
      t.integer :stream_id

      t.timestamps
    end
  end

  def self.down
    drop_table :sources
  end
end
