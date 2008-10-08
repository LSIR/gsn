class PcInit < ActiveRecord::Base

  belongs_to :data_type
  belongs_to :processor
  has_many :pc_parameters, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_presence_of :default_value, :allow_nil => false, :allow_blank => false, :if => Proc.new { |u| ! u.optional }
  validates_inclusion_of :optional, :in => [true, false]
  
  #  def to_label
  #    name
  #  end
 
end
