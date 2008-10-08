class VirtualSensor < ActiveRecord::Base
  has_many :streams, :dependent => :destroy
  belongs_to :pc_instance
  belongs_to :deployment
  has_many :data_selections
end
