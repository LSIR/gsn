class Addressing < ActiveRecord::Base

  belongs_to :addressing_type
  belongs_to :virtual_sensor

  # Validation
  validates_presence_of :value, :allow_nil => false, :allow_blank => false

end
