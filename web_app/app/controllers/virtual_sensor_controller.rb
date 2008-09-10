class VirtualSensorController < ApplicationController

  active_scaffold :virtual_sensors do |config|
#    config.nested.add_link("Streams", [:streams])
#    config.subform.columns.exclude :streams

    end

  def test
    render :partial => 'test', :layout => 'standard'
  end

end
