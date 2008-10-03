class PcInit < ActiveRecord::Base
  belongs_to :data_type
  belongs_to :processor
  has_many :pc_parameters, :dependent => :destroy

  def to_label
    name
  end
 
end
