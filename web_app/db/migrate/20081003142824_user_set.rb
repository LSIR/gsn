class UserSet < ActiveRecord::Migration
  def self.up
    default_admin = User::Admin.new({:email => 'gsn@gsn.sourceforge.net', :password => 'gsn@gsn.sourceforge.net'}).save
  end

  def self.down
    User.delete_all
  end
end
