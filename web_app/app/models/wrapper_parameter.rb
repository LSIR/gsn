class WrapperParameter < ActiveRecord::Base
  belongs_to :wrapper_init
  belongs_to :wrapper_instance
 
  def to_label
    # TODO: ADDING H FUCKTION
    wrapper_init ? "#{wrapper_init.name}=#{value}" : ""
  end
  def default_value
    wrapper_init ? wrapper_init.default_value : ""
  end
  def init_param_name
    wrapper_init ? wrapper_init.name : ""
  end
  def init_param_description
    wrapper_init ? wrapper_init.description : ""
  end

  #Validate
  validates_presence_of :value, :allow_nil => false, :allow_blank => false
  
end
