class AddressingType < ActiveRecord::Base

  has_many :addressings

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

end
