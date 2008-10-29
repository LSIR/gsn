class PropertyGroup < ActiveRecord::Base
  
  has_many :properties
  
  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

end
