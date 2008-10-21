class CreateUsers < ActiveRecord::Migration
  def self.up
    create_table :users do |t|
      t.string :email, :null => false, :limit => 80
      t.string :hashed_password
      t.string :salt
      t.string :type, :default => 'User', :null => false			# Single table inheritance
      t.timestamps
    end

    add_index :users, [:email], :unique

    create_table :deployments_users, :id => false do |t|
      t.references :deployment, :null => false
      t.references :user, :null => false
    end
  end

  def self.down
    drop_table :users
  end
end
