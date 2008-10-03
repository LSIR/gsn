class Source < ActiveRecord::Base
  belongs_to :stream
  has_many :sources_wrapper_instances
  has_many :wrapper_instances, :through=>:sources_wrapper_instances

end
