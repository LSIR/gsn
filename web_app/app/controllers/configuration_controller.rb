class ConfigurationController < ApplicationController

  ##############################################################################
  # User
  ##############################################################################
  
  def user
    render :partial => 'configuration/user/user',
      :layout => 'standard',
      :locals => { :new_user => User.new, :users => User.find(:all) }
  end

  def create_user
    user = User.new(params[:user])
    
    if params[:is_admin]
      user[:type] = Admin.to_s
    else
      user.deployments = Deployment.find(params[:deployment_ids]) if params[:deployment_ids]
    end
    
    if user.save
      flash[:notice] = 'Successfully created the new user'
      redirect_to :action => :user
    else
      flash.now[:notice] = 'Error while saving the user'
      render :partial => '/configuration/user/user',
	      :layout => 'standard',
	      :locals => { :new_user => user, :users => User.find(:all) }
    end
  end

  def update_user
    user = User.find(params[:id])
    
    if params[:user]  
      user.password=(params[:user][:password])
    else
      user.password=(params[:admin][:password])
    end
    
    if params[:deployment_ids]
      user.deployments = Deployment.find(params[:deployment_ids])
    else
      user.deployments = {}
    end
    
    if params[:is_admin]
      user[:type] = Admin.to_s
      user.deployments = {}
    else
      user[:type] = User.to_s
    end
    
    if user.save
      flash[:notice] = 'Successfully updated the user'
      redirect_to :action => :user
    else
      flash.now[:notice] = "Error while saving the user"
      render :partial => '/configuration/user/user',
	      :layout => "standard",
	      :locals => { :new_user => User.new, :users => User.find(:all, :conditions => "id <> #{params[:id]}") << user }
    end
  end

  def delete_user
    User.destroy(params[:id])
    flash[:notice] = 'Sucessfully deleted the user'
    redirect_to :action => :user
  end

  ##############################################################################
  # Processor
  ##############################################################################

  def processor
    render :partial => 'configuration/processor/processor',
      :layout => 'standard',
      :locals => { :processor => Processor.new, :processors => Processor.find(:all)}
  end

  def create_processor
    processor = Processor.new(params[:processor])
    if processor.save
      flash[:notice] = "Successfully created the processor"
      redirect_to :action => :processor
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/processor/processor',
	:layout => 'standard',
	:locals => { :processor => processor, :processors => Processor.find(:all) }
    end
  end

  def update_processor
    params[:processor][:existing_pc_init_attributes] ||= {}
    params[:processor][:existing_output_format_attributes] ||= {}
    params[:processor][:existing_web_input_attributes] ||= {}
    processor = Processor.find(params[:id])
     if processor.update_attributes(params[:processor])
      flash[:notice] = "Successfully updated Processor and its values."
      redirect_to :action => :processor
     else
       flash.now[:notice] = "Not Updated"
       render :partial => '/configuration/processor/processor',
              :layout => "standard",
              :locals => { :processor => Processor.new, :processors => Processor.find(:all, :conditions => "id <> #{params[:id]}") << processor }
     end
  end

  def delete_processor
    proc = Processor.find(params[:id])
    proc.output_formats.each { |o| OutputFormat.destroy(o.id) }
    proc.pc_inits.each { |p| PcInit.destroy(p.id) }
    proc.web_inputs.each { |w| WebInput.destroy(w.id) }
    Processor.delete(params[:id])
    flash[:notice] = "Successfully deleted the Processor"
    redirect_to :action => :processor
  end

  ##############################################################################
  # WrapperInstance
  ##############################################################################
  def wrapper_instance
    render :partial => 'configuration/wrapper_instance/wrapper_instance',
      :layout => 'standard',
      :locals => { :new_wrapper_instance => WrapperInstance.new, :wrapper_instances => WrapperInstance.find(:all) }
  end

  def create_wrapper_instance
    wrapper_instance = WrapperInstance.new(params[:wrapper_instance])
    
    if wrapper_instance.save
      flash[:notice] = "Successfully created the Wrapper Instance and Wrapper Parameters"
      redirect_to :action => :wrapper_instance
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/wrapper_instance/wrapper_instance',
        :layout => "standard",
        :locals => { :new_wrapper_instance => wrapper_instance, :wrapper_instances => WrapperInstance.find(:all) }
    end
  end

  def update_wrapper_instance
    params[:wrapper_instance][:existing_wrapper_parameter_attributes] ||= {}
    wrapper_instance = WrapperInstance.find(params[:id])
    
    if wrapper_instance.update_attributes(params[:wrapper_instance])
      flash[:notice] = "Successfully updated Wrapper Instance and Wrapper Parameters."
      redirect_to :action => :wrapper_instance
    else
      flash.now[:notice] = "Not Updated"
      render :partial => '/configuration/wrapper_instance/wrapper_instance',
        :layout => "standard",
        :locals => { :new_wrapper_instance => WrapperInstance.new, :wrapper_instances => WrapperInstance.find(:all) }
    end
  end

  def delete_wrapper_instance
    WrapperInstance.destroy(params[:id])
    flash[:notice] = "Successfully deleted the Wrapper Instance"
    redirect_to :action => :wrapper_instance
  end

  def select_wrapper
    return render :nothing => true if params[:wrapper_id].to_i == -1
    render :partial => "/configuration/wrapper_instance/form",
      :locals => { :wrapper => Wrapper.find(params[:wrapper_id]),
      :wrapper_instance => WrapperInstance.new }
  end
  
  ##############################################################################
  # Wrapper
  ##############################################################################
  
  def wrapper
    render :partial => '/configuration/wrapper/wrapper',
      :layout => "standard",
      :locals => {  :new_wrapper => Wrapper.new, :wrappers => Wrapper.find(:all) }
  end

  def create_wrapper
    wrapper = Wrapper.new(params[:wrapper])
    
    if wrapper.save
      flash[:notice] = "Successfully created the Wrapper and Wrapper Inits"
      redirect_to :action => :wrapper
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/wrapper/wrapper',
        :layout => "standard",
        :locals => { :new_wrapper => wrapper, :wrappers => Wrapper.find(:all) }
    end
  end

  def update_wrapper
    params[:wrapper][:existing_wrapper_init_attributes] ||= {}
    wrapper = Wrapper.find(params[:id])
    
    if wrapper.update_attributes(params[:wrapper])
      flash[:notice] = "Successfully updated Wrapper and Wrapper Inits."
      redirect_to :action => :wrapper
    else
      flash.now[:notice] = "Not Updated"
      render :partial => "/configuration/wrapper/wrapper",
        :layout => "standard",
        :locals => { :new_wrapper => Wrapper.new, :wrappers => Wrapper.find(:all, :conditions => "id <> #{params[:id]}") << wrapper }
    end
  end

  def delete_wrapper
    Wrapper.destroy(params[:id])
    flash[:notice] = "Successfully deleted the Wrapper"
    redirect_to :action => :wrapper
  end

  ##############################################################################
  # Deployment
  ##############################################################################
  def deployment
    render :partial => 'configuration/deployment/deployment',
      :layout => 'standard',
      :locals => { :new_deployment => Deployment.new, :deployments => Deployment.find(:all) }
  end
  
  def create_deployment
    deployment = Deployment.new(params[:deployment])
    deployment.users = User.find(params[:user_ids]) if (params[:user_ids])
    
    if deployment.save
      flash[:notice] = "Successfully created the deployment"
      redirect_to :action => :deployment
    else
      flash.now[:notice] = "Error while saving the deployment"
      render :partial => '/configuration/deployment/deployment',
        :layout => 'standard',
        :locals => { :new_deployment => deployment, :deployments => Deployment.find(:all) }
    end
  end
  
  def update_deployment
    deployment = Deployment.find(params[:id])
 
    if params[:user_ids]
      deployment.users = User.find(params[:user_ids])
    else
      deployment.users = {}
    end
    
    if deployment.update_attributes params[:deployment] then
       flash[:notice] = 'Successfully updated the deployment'
       redirect_to :action => :deployment
    else
      flash.now[:notice] = "Error while saving the deployment"
      render :partial => '/configuration/deployment/deployment',
            :layout => "standard",
            :locals => { :new_deployment => User.new, :deployments => Deployment.find(:all, :conditions => "id <> #{params[:id]}") << deployment }
    end
  end
  
  def delete_deployment
    Deployment.destroy(params[:id])
    flash[:notice] = 'Sucessfully deleted the deployment'
    redirect_to :action => :deployment
  end

  ##############################################################################
  # VirtualSensor / Stream / Source
  ##############################################################################
  def virtual_sensor
    render :partial => '/configuration/virtual_sensor/virtual_sensor',
      :layout => "standard",
      :locals => {  :new_virtual_sensor => VirtualSensor.new, :virtual_sensors => VirtualSensor.find(:all) }
  end
  
  def create_virtual_sensor
    virtual_sensor = VirtualSensor.new(params[:virtual_sensor])
    
    if virtual_sensor.save
      flash[:notice] = "Successfully created the virtual sensor and its children"
      redirect_to :action => :virtual_sensor
    else
      render :partial => "configuration/virtual_sensor/virtual_sensor", :layout => "standard",
        :locals => { :new_virtual_sensor => virtual_sensor, :virtual_sensors => VirtualSensor.find(:all) }
    end
  end
  
  def update_or_delete_virtual_sensor
    if (params[:delete]) then

      # Delete all the property values belonging to this VirtualSensor
      vs = VirtualSensor.find(params[:id])
      vs.property_values.each { |p| PropertyValue.destroy(p.id) }
      
      # Delete the VirtualSensor itself
      VirtualSensor.destroy(params[:id])

      flash[:notice] = "Successfully deleted the virtual sensor"
      redirect_to :action => :virtual_sensor
    else
      params[:virtual_sensor][:stream_attributes] ||= {}
      params[:virtual_sensor][:stream_attributes].each { |key, value|
        params[:virtual_sensor][:stream_attributes][key][:source_attributes] ||= {} unless key == 'new'
      }
      
      virtual_sensor = VirtualSensor.find(params[:id])
      
      if virtual_sensor.update_attributes params[:virtual_sensor] then
        flash[:notice] = "Successfully updated the virtual sensor"
        redirect_to :action => :virtual_sensor
      else
        flash[:notice] = "Error while saving the virtual sensor"
        render :partial => "configuration/virtual_sensor/virtual_sensor", :layout => "standard",
          :locals => { :new_virtual_sensor => VirtualSensor.new, :virtual_sensors => VirtualSensor.find(:all, :conditions => "id <> #{params[:id]}") << virtual_sensor }
      end
    end
  end
    
  def form_for_stream
    render :partial => '/configuration/virtual_sensor/form_for_stream',
      :locals => { :stream => Stream.new, :prefix => params[:prefix] || '' }
  end
  
  def form_for_source
    render :partial => '/configuration/virtual_sensor/form_for_source',
      :locals => { :source => Source.new, :prefix => params[:prefix] || '' }
  end

  ##############################################################################
  # Property / PropertyGroup
  ##############################################################################
  def property
    render :partial => 'configuration/property/property',
           :layout => 'standard',
           :locals => { :property_group => PropertyGroup.new,
                        :property_groups => PropertyGroup.find(:all),
                        :properties => Property.find(:all),
                        :property_values => PropertyValue.find(:all)  }
  end
  
  def create_property_group
    property_group = PropertyGroup.new(params[:property_group])
    if property_group.save
      flash[:notice] = "Successfully created the property group."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Created"
      render :partial => 'configuration/property/property',
             :layout => 'standard',
             :locals => { :property_group => property_group,
                          :property_groups => PropertyGroup.find(:all),
                          :properties => Property.find(:all),
                          :property_values => PropertyValue.find(:all)  }
    end
  end

  def update_property_group
    property_group = PropertyGroup.find(params[:id])
    if property_group.update_attributes(params[:property_group])
      flash[:notice] = "Successfully updated the property group."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Updated"
      render :partial => 'configuration/property/property',
             :layout => 'standard',
             :locals => { :property_group => property_group,
                          :property_groups => PropertyGroup.find(:all),
                          :properties => Property.find(:all),
                          :property_values => PropertyValue.find(:all)  }
    end
  end
  
  def delete_property_group
    PropertyGroup.delete(params[:id])
    flash[:notice] = "Successfully deleted the property group"
    redirect_to :action => :property
  end

  def create_property
    @property = Property.new(params[:property])
    if @property.save
      flash[:notice] = "Successfully created property."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Created"
      redirect_to :action => :property
    end
  end
  
  def update_property
    @property = Property.find(params[:id])
    if @property.update_attributes(params[:property])
      flash[:notice] = "Successfully updated property and properties."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Updated"
      redirect_to :action => :property
    end
  end

  def delete_property
    Property.delete(params[:id])
    flash[:notice] = "Successfully deleted the property"
    redirect_to :action => :property
  end

  def create_property_value
    @property_value = PropertyValue.new(params[:property_value])
    if @property_value.save
      flash[:notice] = "Successfully created property value."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Created"
      redirect_to :action => :property
    end
  end

  def update_property_value
    @property_value = PropertyValue.find(params[:id])
    if @property_value.update_attributes(params[:property_value])
      flash[:notice] = "Successfully updated the property value."
      redirect_to :action => :property
    else
      flash.now[:notice] = "Not Updated"
      redirect_to :action => :property
    end
  end
  
  def delete_property_value
    PropertyValue.delete(params[:id])
    flash[:notice] = "Successfully deleted the property value"
    redirect_to :action => :property
  end

  ##############################################################################
  # PCInstance
  ##############################################################################
  def pc_instance
    render :partial => 'configuration/pc_instance/pc_instance',
      :layout => 'standard',
      :locals => { :pc_instance => PcInstance.new, :processors => Processor.find(:all), :pc_instances => PcInstance.find(:all)}
  end

  def create_pc_instance
      pc_instance = PcInstance.new(params[:pc_instance])
    if pc_instance.save
      flash[:notice] = "Successfully created the processor instance"
      redirect_to :action => :pc_instance
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/pc_instance/pc_instance',
        :layout => 'standard',
        :locals => { :pc_instance => pc_instance, :processors => Processor.find(:all), :pc_instances => PcInstance.find(:all)}
    end
  end

  def update_pc_instance
    params[:pc_instance][:existing_pc_parameter_attributes] ||= {}
    pc_instance = PcInstance.find(params[:id])
     if pc_instance.update_attributes(params[:pc_instance])
      flash[:notice] = "Successfully updated Pc Instance and its values."
      redirect_to :action => :pc_instance
     else
       flash.now[:notice] = "Not Updated"
       render :partial => '/configuration/pc_instance/pc_instance',
              :layout => "standard",
              :locals => { :pc_instance => PcInstance.new, :processors => Processor.find(:all), :pc_instances => PcInstance.find(:all, :conditions => "id <> #{params[:id]}") << pc_instance }
     end
  end

  def delete_pc_instance
    pci = PcInstance.find(params[:id])
    pci.pc_parameters.each { |p| PcParameter.destroy(p.id) }
    PcInstance.delete(params[:id])
    flash[:notice] = "Successfully deleted the PcInstance"
    redirect_to :action => :pc_instance
  end

  def form_for_parameter
    pcinits = PcInit.find(:all,:conditions => "processor_id = #{params[:procid]}")
    render :partial => '/configuration/pc_instance/pc_parameter',
    :locals => { :pcinits => pcinits, :pc_parameter => PcParameter.new}
  end
end
