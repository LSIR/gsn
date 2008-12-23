class Source < ActiveRecord::Base

  belongs_to :stream
  has_many :sources_wrapper_instances, :dependent => :destroy
  has_many :wrapper_instances, :through => :sources_wrapper_instances#, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_presence_of :query, :allow_nil => false, :allow_blank => false
  validates_ratio :load_shedding
  validates_numericality_of :sliding, :window_size
  validates_numericality_of :window_size, :greater_than => 0

end
