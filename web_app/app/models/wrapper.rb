class Wrapper < ActiveRecord::Base
  has_many :wrapper_inits, :dependent => :destroy
  has_many :wrapper_instances, :dependent => :destroy
  def blah
    "TODO"
  end

  # Validation
  validates_presence_of :class_name, :name, :description, :allow_nil => false, :allow_blank => false
  validates_length_of :name, :in => 5..20
  validates_length_of :description, :class_name, :maximum => 250
  validates_uniqueness_of :name

  #
  def new_wrapper_init_attributes=(wrapper_inits_attributes)
    wrapper_inits_attributes.each do |attributes|
      wrapper_inits.build(attributes)
    end
  end

  after_update :save_wrapper_inits

  def existing_wrapper_init_attributes=(wrapper_inits_attributes)
  wrapper_inits.reject(&:new_record?).each do |init|
      attributes = wrapper_inits_attributes[init.id.to_s]
      if attributes
	init.attributes = attributes
      else
	wrapper_inits.delete(init)
      end
    end
  end

  def save_wrapper_inits
    wrapper_inits.each do |init|
      init.save(false)
    end
  end
end
