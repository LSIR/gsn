require 'digest/sha1'

class User < ActiveRecord::Base

  has_and_belongs_to_many :deployments

  # Validation
  validates_email :email
  validates_uniqueness_of :email
  validates_confirmation_of :password

  #
  attr_accessor :password_confirmation

  def validate
    errors.add_to_base("Missing password") if hashed_password.blank?
  end

  def self.authenticate(email, password)
    user = self.find_by_email(email)
    if user
      exprected_password = encripted_password(password, user.salt)
      if user.hashed_password != exprected_password
        user = nil
      end
    end
    user
  end

  def password
    @password
  end

  def password=(pwd)
    @password = pwd
    create_new_salt
    self.hashed_password = User.encripted_password(self.password, self.salt)
  end

  private

  def self.encripted_password(password, salt)
    string_to_hash = password + "gsnpass" + salt
    Digest::SHA1.hexdigest(string_to_hash)
  end

  def create_new_salt
    self.salt = self.object_id.to_s + rand.to_s
  end
end

class Admin < User
  has_many :deployments
end
