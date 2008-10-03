class PcInstance < ActiveRecord::Base
  has_many :pc_parameters, :dependent => :destroy
  belongs_to :processor
  has_many :virtual_sensors, :dependent => :destroy

  def processor_name
    processor.name
  end
  def to_label 
    name
  end
 
end
