class Param < ActiveRecord::Base
  # name
  # description
  belongs_to :data_type
  belongs_to :parametrizable
end



