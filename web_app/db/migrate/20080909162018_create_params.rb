class CreateParams < ActiveRecord::Migration
  def self.up
    create_table :params do |t|
      t.string :name
      t.text :description
      t.string :user_description
      t.string :default_value
      t.string :user_value
      t.boolean :optional
      t.integer :data_type_id
      t.integer :parametrizable_id
      t.string :type #required for single table inheritance
    end
  end

  def self.down
    drop_table :params
  end
end
