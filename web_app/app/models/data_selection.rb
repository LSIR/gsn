class DataSelection < ActiveRecord::Base
  belongs_to :data_configuration
  belongs_to :output_format
  has_many :criterions
  belongs_to :virtual_sensor
end
