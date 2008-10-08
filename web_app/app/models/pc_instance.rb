class PcInstance < ActiveRecord::Base

  has_many :pc_parameters, :dependent => :destroy
  belongs_to :processor
  has_many :virtual_sensors, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

#  def processor_name
#    processor.name
#  end
#  def to_label
#    name
#  end
 
end
