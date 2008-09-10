class Source < ActiveRecord::Base
  has_many :wrappers, :foreign_key => :resource_id
  belongs_to :stream
end
