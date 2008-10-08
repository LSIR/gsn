class VirtualSensor < ActiveRecord::Base
  has_many :streams, :dependent => :destroy
  belongs_to :pc_instance
  belongs_to :deployment
  has_many :data_selections, :dependent => :destroy

  # Validations
  validates_identifier :name
  validates_uniqueness_of :name
  validates_numericality_of :priority, :pool_size, :greater_than => 0
  validates_percentage :load_shedding
  validates_storage_size :storage_size
  validates_inclusion_of :protected, :unique_timestamp, :in => [true, false]

end
