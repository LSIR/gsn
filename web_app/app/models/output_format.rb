class OutputFormat < ActiveRecord::Base

  belongs_to :data_type
  belongs_to :processor
  belongs_to :unit
  
  has_many :property_values, :as => :prop_value_owner

  # Validation
  validates_identifier :name

end
