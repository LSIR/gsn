class CreateDataConfigurations < ActiveRecord::Migration
  def self.up
    create_table :data_configurations do |t|
      t.integer :user_id
      t.string :name
      t.string :from
      t.string :to
      t.integer :nb
      t.string :aggregation
      t.timestamps
    end
  end

  def self.down
    drop_table :data_configurations
  end
end
