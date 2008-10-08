module CustomValidations

  def validates_identifier(*attr_names)
    attr_names.each do |attr_name|
      validates_format_of attr_name,
	:with => /^([\w])+$/,
	:message => 'Must be an identifier composed of characters in [A-Za-z0-9_]'
    end
  end

  def validates_ratio(*attr_names)
    attr_names.each do |attr_name|
      validates_numericality_of attr_name,
	:greater_than_or_equal_to => 0,
	:less_than_or_equal_to => 1,
	:message => 'Must be a ratio in [0.0 , 1.0]'
    end
  end

  def validates_storage_size(*attr_names)
    attr_names.each do |attr_name|
      validates_format_of attr_name,
	:with => /^(\d)+([d|h|s|m])?$/,
	:message => 'Must be an integer optionally suffixed by a timed unit in [d|h|s|m]'
    end
  end

  def validates_class_name(*attr_names)
    attr_names.each do |attr_name|
      validates_format_of attr_name,
	:with => /^((([\w])+\.?)+\w)$/,
	:message => 'Must be a class name, including the package path'
    end
  end

  def validates_email(*attr_names)
    attr_names.each do |attr_name|
      validates_format_of attr_name,
	:with => /\A([^@\s]+)@((?:[-a-z0-9]+\.)+[a-z]{2,})\Z/i,
	:message => 'Is not a valid email address'
    end
  end

  

end
ActiveRecord::Base.extend(CustomValidations)
