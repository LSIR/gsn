class DataSelection < ActiveRecord::Base

  belongs_to :data_configuration
  belongs_to :output_format
  has_many :criterions, :dependent => :destroy
  belongs_to :virtual_sensor

  # Validation

end
