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
  
  def vsa2
    to_return = {}
    VirtualSensor.find(:all).each do |vs|
      to_return[vs.name.to_sym] ||= {}
      vs.property_values.each do |prop|
        to_return[vs.name.to_sym][prop.property.name.to_sym] = prop.value.to_sym
      end
      to_return[vs.name.to_sym][:deployment] = vs.deployment.name.to_sym
      to_return[vs.name.to_sym][:id] = vs.id
    end
    # p to_return
    render :json => to_return, :layout => false
  end
  
  def test
    ff1 = nil
    ff = nil
    #render :json => VirtualSensor.find(:all), :layout => false
    VirtualSensor.find(:all).each do |tmp|
            ff1 = ff
      ff = tmp

    end
          render :json => ff1.property_values.find(:all), :layout => false
  end

end
