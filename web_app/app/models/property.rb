class Property < ActiveRecord::Base
  
    belongs_to :property_type
    
    # Validation
    validates_presence_of :name, :allow_nil => false, :allow_blank => false
    
end
