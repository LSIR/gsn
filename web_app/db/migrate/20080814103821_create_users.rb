class CreateUsers < ActiveRecord::Migration
  def self.up
    create_table    :gsn_users do |t|
      t.string	    :email,		:null => false,	:limit => 60
      t.string	    :hashed_password,	:null => false
      t.string	    :salt,		:null => false
      t.datetime    :created_at,	:null => false
      t.datetime    :updated_at,	:null => false
    end
  end

  def self.down
    drop_table :users
  end
end
