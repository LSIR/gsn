class WrapperInit < ActiveRecord::Base

  belongs_to :wrapper
  belongs_to :data_type
  has_many :wrapper_parameters, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_inclusion_of :optional, :in => [true, false]
  validates_presence_of :default_value, :allow_nil => false, :allow_blank => false, :if => Proc.new { |u| ! u.optional }

end
