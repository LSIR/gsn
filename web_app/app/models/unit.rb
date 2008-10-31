class Unit < ActiveRecord::Base
  
  has_many :properties
  has_many :output_formats
  
  # Validation
  validates_presence_of :name, :allow_nil => false, :allow_blank => false
end
