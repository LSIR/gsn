class SourcesWrapperInstance < ActiveRecord::Base
  belongs_to :wrapper_instances
  belongs_to :sources
end
