class Stream < ActiveRecord::Base
  has_many :sources, :dependent => :destroy
  belongs_to :virtual_sensor
end
