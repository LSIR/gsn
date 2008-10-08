class DataConfiguration < ActiveRecord::Base

  belongs_to :user
  has_many :data_selections, :dependent => :destroy

  # Validation
  validates_identifier :name
  validates_uniqueness_of :name
  validates_presence_of :from, :to, :nb, :aggregation
  validates_inclusion_of :aggregation, :in => %w( max min avg )

end
