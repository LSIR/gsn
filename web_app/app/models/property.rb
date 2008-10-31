class Property < ActiveRecord::Base
  
    belongs_to :property_group
    belongs_to :unit
    
    has_many :property_values
    
    # Validation
    validates_presence_of :name, :allow_nil => false, :allow_blank => false
    
end
