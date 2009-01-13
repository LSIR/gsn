class WebInput < ActiveRecord::Base

  belongs_to :processor
  has_many :web_commands, :dependent => :destroy

  # Validation
  validates_identifier :name
  
  #
  after_update :save_web_commands

  #
  def new_web_command_attributes=(web_command_attributes)
    web_command_attributes.each do |attributes|
      web_commands.build(attributes)
    end
  end

  def existing_web_command_attributes=(web_command_attributes)
    web_command.reject(&:new_record?).each do |init|
      attributes = web_command_attributes[init.id.to_s]
      if attributes
	init.attributes = attributes
      else
	web_commands.delete(init)
      end
    end
  end

  def web_commands_attributes
    web_commands.each do |init|
      init.save(false)
    end
  end

def save_web_commands
    web_commands.each do |param|
      param.save(false)
    end
  end







end
