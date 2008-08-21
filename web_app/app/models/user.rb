require 'digest/sha1'

class User < ActiveRecord::Base

  validates_presence_of	    :email
  validates_uniqueness_of   :email
  validates_format_of	    :email,	:with => /\A([^@\s]+)@((?:[-a-z0-9]+\.)+[a-z]{2,})\Z/i

  validates_length_of	    :password,	:minimum => 6
  validates_confirmation_of :password,	:message => "Should match confirmation.",	:on => :update

  def validate
    errors.add_to_base("Missing Password") if hashed_password.blank?
  end

  def password
    @password
  end

  def password=(pwd)
    @password = pwd
    create_new_salt
    self.hashed_password = User.encrypted_password(self.password, self.salt)
  end

  def self.authenticate(email, password)
    user = self.find_by_email(email)
    if user
      expected_password = encrypted_password(password, user.salt)
      if user.hashed_password != expected_password
	user = nil
      end
    end
    user
  end

  private

  def self.encrypted_password(password, salt)
    string_to_hash = password + "gsn_nut" + salt
    Digest::SHA1.hexdigest(string_to_hash)
  end

  def create_new_salt
    self.salt = self.object_id.to_s + rand.to_s
  end

end
