class Deployment < ActiveRecord::Base

  belongs_to :admin
  has_and_belongs_to_many :users
  has_many :virtual_sensors, :dependent => :destroy
  has_many :property_values, :as => :prop_value_owner

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_inclusion_of :private, :in => [true, false]
  validates_presence_of :admin_id, :nil => false, :blank => false

end
