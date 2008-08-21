class DefaultAdministrator < ActiveRecord::Migration
  def self.up
    user = User.find_by_email(GSN_DEFAULT_ADMIN_EMAIL)
    if user.nil?
	user = User.new({:email => GSN_DEFAULT_ADMIN_EMAIL, :password => GSN_DEFAULT_ADMIN_PASSWORD})
	user.save if not user.nil?
    end
  end

  def self.down
    user = User.find_by_email(GSN_DEFAULT_ADMIN_EMAIL)
    user.destroy if not user.nil?
  end
end
