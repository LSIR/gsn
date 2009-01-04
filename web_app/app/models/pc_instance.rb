class PcInstance < ActiveRecord::Base

  has_many :pc_parameters, :dependent => :destroy
  belongs_to :processor
  has_many :virtual_sensors, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

  def new_pc_parameter_attributes=(pc_parameter_attributes)
    pc_parameter_attributes.each do |attributes|
      pc_parameters.build(attributes)
    end
  end

  def existing_virtual_sensor_attributes=(virtual_sensor_attributes)
    virtual_sensor_attributes.each do |attributes|
      virtual_sensors.build(attributes)
    end
  end
#  def processor_name
#    processor.name
#  end
#  def to_label
#    name
#  end
 
end
