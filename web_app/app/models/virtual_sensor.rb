class VirtualSensor < ActiveRecord::Base
  has_many :streams, :dependent => :destroy
  belongs_to :pc_instance
end
