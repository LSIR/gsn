class MapController < ApplicationController

  skip_before_filter :login_required

  # Returns a json object which contains the virtual sensors and their addressing fields.
  def vsa
    to_return = {}
    VirtualSensor.find(:all).each do |vs|
      to_return[vs.name.to_sym] ||= {}
      vs.addressings.each do |addressing|
        to_return[vs.name.to_sym][addressing.addressing_type.name.to_sym] = addressing.value.to_sym
      end
      to_return[vs.name.to_sym][:deployment] = vs.deployment.name.to_sym
      to_return[vs.name.to_sym][:id] = vs.id
    end
    # p to_return
    render :json => to_return, :layout => false
  end

end
