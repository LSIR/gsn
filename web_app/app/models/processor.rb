class Processor < ActiveRecord::Base

  has_many :pc_inits, :dependent => :destroy
  has_many :output_formats, :dependent => :destroy
  has_many :web_inputs, :dependent => :destroy
  has_many :pc_instances, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_class_name :class_name
  validates_associated :pc_inits
  validates_associated :output_formats
  validates_associated :web_inputs

  #
  after_update :save_output_formats, :save_pc_inits, :save_web_inputs

  def new_output_format_attributes=(output_format_attributes)
    output_format_attributes.each do |attributes|
      output_formats.build(attributes)
    end
  end

  def existing_output_format_attributes=(output_format_attributes)
    output_formats.reject(&:new_record?).each do |init|
      attributes = output_format_attributes[init.id.to_s]
      if attributes
        init.attributes = attributes
      else
        output_formats.delete(init)
      end
    end
  end

  def output_format_attributes
    output_formats.each do |init|
      init.save(false)
    end
  end

  #
  def new_pc_init_attributes=(pc_init_attributes)
    pc_init_attributes.each do |attributes|
      pc_inits.build(attributes)
    end
  end

  def existing_pc_init_attributes=(pc_init_attributes)
    pc_inits.reject(&:new_record?).each do |init|
      attributes = pc_init_attributes[init.id.to_s]
      if attributes
	init.attributes = attributes
      else
	pc_inits.delete(init)
      end
    end
  end

  def pc_inits_attributes
    pc_inits.each do |init|
      init.save(false)
    end
  end

  #

  def new_web_input_attributes=(web_input_attributes)
    web_input_attributes.each do |attributes|
      web_inputs.build(attributes)
    end
  end

  def existing_web_input_attributes=(web_input_attributes)
    web_inputs.reject(&:new_record?).each do |init|
      attributes = web_input_attributes[init.id.to_s]
      if attributes
	init.attributes = attributes
      else
	web_inputs.delete(init)
      end
    end
  end

  def web_inputs_attributes
    web_inputs.each do |init|
      init.save(false)
    end
  end

  def save_output_formats
    output_formats.each do |param|
      param.save(false)
    end
  end

  def save_pc_inits
    pc_inits.each do |param|
      param.save(false)
    end
  end

  def save_web_inputs
    web_inputs.each do |param|
      param.save(false)
    end
  end
  # temp
end
