class PcInstance < ActiveRecord::Base

  has_many :pc_parameters, :dependent => :destroy
  belongs_to :processor
  has_many :virtual_sensors, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_presence_of :web_password
  validates_presence_of :processor_id
  validates_associated :pc_parameters

  def new_pc_parameter_attributes=(pc_parameter_attributes)
    pc_parameter_attributes.each do |attributes|
      pc_parameters.build(attributes)
    end
  end

  after_update :save_pc_parameters
  def existing_pc_parameter_attributes=(pc_parameter_attributes)
    pc_parameters.reject(&:new_record?).each do |pc_parameter|
      attributes = pc_parameter_attributes[pc_parameter.id.to_s]
      if attributes
        pc_parameter.attributes = attributes
      else
        pc_parameters.delete(pc_parameter)
      end
    end
  end

   def save_pc_parameters
    pc_parameters.each do |param|
      param.save(false)
    end
  end
#  def processor_name
#    processor.name
#  end
#  def to_label
#    name
#  end
 
end
