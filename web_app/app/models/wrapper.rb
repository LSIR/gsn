class Wrapper < ActiveRecord::Base

  has_many :wrapper_inits, :dependent => :destroy
  has_many :wrapper_instances, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_class_name :class_name

  validates_associated :wrapper_inits

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
