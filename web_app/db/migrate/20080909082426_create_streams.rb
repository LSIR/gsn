class CreateStreams < ActiveRecord::Migration
  def self.up
    create_table :streams do |t|
      t.string :name
      t.text :query
      t.integer :virtual_sensor_id

      t.timestamps
    end
  end

  def self.down
    drop_table :streams
  end
end
