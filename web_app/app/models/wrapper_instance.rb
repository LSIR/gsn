class WrapperInstance < ActiveRecord::Base

  belongs_to :wrapper
  has_many :wrapper_parameters, :dependent => :destroy
  has_many :sources_wrapper_instances, :dependent => :destroy
  has_many :sources, :through=>:sources_wrapper_instances, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

  #
  def new_wrapper_parameter_attributes=(wrapper_parameters_attributes)
    wrapper_parameters_attributes.each do |attributes|
      wrapper_parameters.build(attributes)
    end
  end

  after_update :save_wrapper_parameters

  def existing_wrapper_parameter_attributes=(wrapper_parameters_attributes)
    wrapper_parameters.reject(&:new_record?).each do |init|
      attributes = wrapper_parameters_attributes[init.id.to_s]
      if attributes
	init.attributes = attributes
      else
	wrapper_parameters.delete(init)
      end
    end
  end

  def save_wrapper_parameters
    wrapper_parameters.each do |init|
      init.save(false)
    end
  end

end
