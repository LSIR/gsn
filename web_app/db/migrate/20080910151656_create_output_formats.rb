class CreateOutputFormats < ActiveRecord::Migration
  def self.up
    create_table :output_formats do |t|
      t.string :name
      t.string :description
      t.integer :data_type_id
      t.integer :processor_id
      t.timestamps
    end
  end

  def self.down
    drop_table :output_formats
  end
end
