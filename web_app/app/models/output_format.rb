class OutputFormat < ActiveRecord::Base
  belongs_to :data_type
  belongs_to :processor
end
