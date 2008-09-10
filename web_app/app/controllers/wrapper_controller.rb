class WrapperController < ApplicationController
    active_scaffold :wrappers do |config|
      config.actions.exclude :search
      config.create.columns.exclude [:source,:type]
      config.columns = [:name,:description,:params]
#    config.nested.add_link("Streams", [:streams])
#    config.subform.columns.exclude :streams
     #config.theme = :gsn
    end

  def test
    render :partial => 'test', :layout => 'standard'
  end
end
