class PcParameter < ActiveRecord::Base
  belongs_to :pc_init
  belongs_to :pc_instance

  def param_name
    pc_init ? pc_init.name : "" 
  end
  def param_description
    pc_init ? pc_init.description : ""
  end
  def param_default_value
    pc_init ? pc_init.default_value : ""
  end
  def param_optional
    pc_init ? pc_init.optional : ""
  end
  def to_label 
    "#{param_name}#{value.blank? ? '' : '='}#{value}"
  end
end
