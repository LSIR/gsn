class WebCommand < ActiveRecord::Base

  belongs_to :web_input

  # Validation
  validates_identifier :name
  validates_presence_of :rule, :allow_nil => false, :allow_blank => false

end
