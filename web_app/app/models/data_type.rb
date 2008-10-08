class DataType < ActiveRecord::Base

  has_many :wrapper_inits, :dependent => :destroy
  has_many :pc_inits, :dependent => :destroy
  has_many :output_formats, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name

end
