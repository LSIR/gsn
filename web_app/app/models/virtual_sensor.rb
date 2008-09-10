class VirtualSensor < ActiveRecord::Base
  has_one :pc_initialize, :foreign_key => :resource_id
  has_many :pc_web_commands, :foreign_key => :resource_id
  has_one :output_format,  :foreign_key => :resource_id
  has_many :streams
end
