class Stream < ActiveRecord::Base
  has_many :sources
  belongs_to :virtual_sensor
end
