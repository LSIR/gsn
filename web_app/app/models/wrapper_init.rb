class WrapperInit < ActiveRecord::Base

  belongs_to :wrapper
  belongs_to :data_type
  has_many :wrapper_parameters, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name, :scope => :wrapper_id
  validates_inclusion_of :optional, :in => [true, false]
  validates_presence_of :default_value, :allow_nil => false, :allow_blank => false, :if => Proc.new { |wrapper_init| wrapper_init.optional }

end
