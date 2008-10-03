class DataType < ActiveRecord::Base
  has_many :wrapper_inits, :dependent => :destroy
  has_many :pc_inits, :dependent => :destroy
  has_many :output_formats, :dependent => :destroy
  # Validation 
  validates_presence_of :name, :description, :allow_nil => false, :allow_blank => false
  validates_length_of :name, :in => 3..10
  validates_length_of :description, :maximum => 250
  validates_uniqueness_of :name
end
