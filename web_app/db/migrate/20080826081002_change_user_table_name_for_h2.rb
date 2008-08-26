class ChangeUserTableNameForH2 < ActiveRecord::Migration
  def self.up
    rename_table(:users, :gsn_users)
  end

  def self.down
    rename_table(:gsn_users, :users)
  end
end
