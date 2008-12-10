class PropertyGroup < ActiveRecord::Base
  
  has_many :properties, :dependent => :destroy

   # Validation
  validates_identifier :name
  validates_uniqueness_of :name

  def new_property_attributes=(property_attributes)
    property_attributes.each do |attributes|
      properties.build(attributes)
    end
  end

  after_update :save_properties

  def existing_property_attributes=(property_attributes)
    properties.reject(&:new_record?).each do |property|
      attributes = property_attributes[property.id.to_s]
      if attributes
        property.attributes = attributes
      else
        properties.delete(property)
      end
    end
  end

  def save_properties
    properties.each do |property|
      property.save(false)
    end
  end
 
end
