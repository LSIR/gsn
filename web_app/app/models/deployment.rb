class Deployment < ActiveRecord::Base
  belongs_to :admin
  has_and_belongs_to_many :users
  has_many :virtual_sensors
end
