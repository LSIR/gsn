class WrapperInit < ActiveRecord::Base
  belongs_to :wrapper
  belongs_to :data_type
  has_many :wrapper_parameters, :dependent => :destroy
  # Validation
  validates_presence_of :name, :description, :optional, :allow_nil => false, :allow_blank => false
  validates_presence_of :default_value, :if => Proc.new { |u| ! u.optional }
  validates_length_of :name, :in => 3..20
  validates_length_of :description, :maximum => 250
end
