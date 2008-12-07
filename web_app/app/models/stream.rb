class Stream < ActiveRecord::Base

  has_many :sources, :dependent => :destroy, :attributes => true
  belongs_to :virtual_sensor

  # Validation
  validates_identifier :name
  validates_presence_of :query, :allow_nil => false, :allow_blank => false


end
