class Criterion < ActiveRecord::Base

  belongs_to :data_selection

  # Validation
  validates_inclusion_of :not, :in => [true, false]
  validates_presence_of :operator, :join, :value
  validates_numericality_of :value
  validates_inclusion_of :operator, :in => %w( le leq ge geq eq like )
  validates_inclusion_of :join, :in => %w( or and )

end
