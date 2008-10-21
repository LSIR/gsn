class Deployment < ActiveRecord::Base

  belongs_to :admin
  has_and_belongs_to_many :users
  has_many :virtual_sensors, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_inclusion_of :private, :in => [true, false]

end
