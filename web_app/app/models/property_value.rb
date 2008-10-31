class PropertyValue < ActiveRecord::Base
  
  belongs_to :prop_value_owner, :polymorphic => true
  belongs_to :property

end
