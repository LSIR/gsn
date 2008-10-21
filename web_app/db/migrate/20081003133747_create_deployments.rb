class CreateDeployments < ActiveRecord::Migration
  def self.up
    create_table :deployments do |t|
      t.references :admin
      t.boolean :private
      t.string :name
      t.text :description
      t.timestamps
    end

    add_index :deployments, [:name], :unique

  end

  def self.down
    drop_table :deployments
  end
end
