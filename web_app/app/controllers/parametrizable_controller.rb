
class ParametrizableController < ApplicationController
      active_scaffold :parametrizable do |config|
	config.create.columns.exclude [:type]
#    config.nested.add_link("Streams", [:streams])
#    config.subform.columns.exclude :streams
    end

end
