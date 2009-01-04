class ConfigurationController < ApplicationController

  # User

  def user
    render :partial => 'configuration/user/user',
      :layout => 'standard',
      :locals => { :user => User.new, :users => User.find(:all) }
  end

  def create_user
    user = User.new(params[:user])
    user.deployments = Deployment.find(params[:deployment_ids]) if params[:deployment_ids]
    if user.save
      flash[:notice] = 'Successfully created the new user'
      redirect_to :action => :user
    else
      flash.now[:notice] = 'Not Created'
      render :partial => '/configuration/user/user',
	:layout => 'standard',
	:locals => { :user => user }
    end
  end

  def update_user
    user = User.find(params[:id])
    user.password=(params[:user][:password])
    if params[:deployment_ids]
      user.deployments = Deployment.find(params[:deployment_ids])
    else
      user.deployments = {}
    end
    if user.save
      flash[:notice] = 'Successfully updated the user'
      redirect_to :action => :user
    else
      flash.now[:notice] = "Not Updated"
      render :partial => '/configuration/user/user',
	:layout => "standard",
	:locals => { :user => user, :deployments => Deployment.find(:all) }
    end
  end

  def delete_user
    User.delete(params[:id])
    flash[:notice] = 'Sucessfully deleted the user'
    redirect_to :action => :user
  end

  # Processor

  def processor
    render :partial => 'configuration/processor/processor',
      :layout => 'standard',
      :locals => { :processor => Processor.new }
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
	:locals => { :processor => processor }
    end
  end

  # wrapper_instance

  def wrapper_instance
    render :partial => 'configuration/wrapper_instance/wrapper_instance',
      :layout => 'standard',
      :locals => { :wrapper_instance => WrapperInstance.new, :wrapper_instances => WrapperInstance.find(:all) }
  end

  def create_wrapper_instance
    wrapper_instance = WrapperInstance.new(params[:wrapper_instance])
    if wrapper_instance.save
      flash[:notice] = "Successfully created the wrapper_instance and wrapper_parameters"
      redirect_to :action => :wrapper_instance
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/wrapper_instance/wrapper_instance',
	:layout => "standard",
	:locals => { :wrapper_instance => wrapper_instance, :wrapper_instances => WrapperInstance.find(:all) }
    end
  end

  def update_wrapper_instance
    params[:wrapper_instance][:existing_wrapper_parameter_attributes] ||= {}
    wrapper_instance = WrapperInstance.find(params[:id])
    if wrapper_instance.update_attributes(params[:wrapper_instance])
      flash[:notice] = "Successfully updated wrapper_instance and wrapper_parameters."
      redirect_to :action => :wrapper_instance
    else
      flash.now[:notice] = "Not Updated"
      render :partial => '/configuration/wrapper_instance/wrapper_instance',
	:layout => "standard",
	:locals => { :wrapper_instance => wrapper_instance, :wrapper_instances => WrapperInstance.find(:all) }
    end
  end

  def delete_wrapper_instance
    WrapperInstance.delete(params[:id])
    flash[:notice] = "Successfully deleted the wrapper_instance"
    redirect_to :action => :wrapper_instance
  end

  def select_wrapper
    return render :nothing => true if params[:wrapper_id].to_i == -1
    render :partial => "/configuration/wrapper_instance/form",
      :locals => { :wrapper => Wrapper.find(params[:wrapper_id]),
      :wrapper_instance => WrapperInstance.new }
  end



  # wrapper

  def wrapper
    render :partial => '/configuration/wrapper/wrapper',
      :layout => "standard",
      :locals => {  :wrapper => Wrapper.new, :wrappers => Wrapper.find(:all) }
  end

  def create_wrapper
    wrapper = Wrapper.new(params[:wrapper])
    if wrapper.save
      flash[:notice] = "Successfully created the wrapper and wrapper_inits"
      redirect_to :action => :wrapper
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/wrapper/wrapper',
	:layout => "standard",
	:locals => { :wrapper => wrapper, :wrappers => Wrapper.find(:all) }
    end
  end

  def update_wrapper
    params[:wrapper][:existing_wrapper_init_attributes] ||= {}
    wrapper = Wrapper.find(params[:id])
    if wrapper.update_attributes(params[:wrapper])
      flash[:notice] = "Successfully updated wrapper and wrapper_inits."
      redirect_to :action => :wrapper
    else
      flash.now[:notice] = "Not Updated"
      render :partial => "/configuration/wrapper/form",
	:layout => "standard",
	:locals => { :wrapper => wrapper }
    end
  end

  def delete_wrapper
    Wrapper.delete(params[:id])
    flash[:notice] = "Successfully deleted the wrapper"
    redirect_to :action => :wrapper
  end

  ##########################################################
  #           DEPLOYMENT MANAGEMENT RELATED METHODS        #
  ##########################################################
  #uses_tiny_mce

  def deployment
    render :partial => 'configuration/deployment/deployment',
      :layout => 'standard',
      :locals => { :deployment => Deployment.new, :virtualsensors => VirtualSensor.find(:all) }
  end
  
  def create_deployment
    deployment = Deployment.new(params[:deployment])
    if deployment.save
      flash[:notice] = "Successfully created the deployment"
      redirect_to :action => :deployment
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/deployment/deployment',
        :layout => 'standard',
        :locals => { :deployment => deployment }
    end
  end
  def form_for_property_value
    properties = Property.find(:all,:conditions => "property_group_id = #{params[:pg_id]}")
    render :partial => '/configuration/deployment/property_value',
    :locals => { :properties => properties , :property_value => PropertyValue.new, :vs => VirtualSensor.find(:all), :out_format => OutputFormat.find(:all)}
  end
  
  # Virtual Sensor
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
        flash[:notice] = "Error while saving virtual sensor"
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

  #Property_group
  
  def property_group
    @property_group = PropertyGroup.new
    @property_group.properties.build
    render :partial => 'configuration/property_group/property_group',
    :layout => 'standard',
    :locals => {:properties_group => PropertyGroup.find(:all)}
  end
  
  def create_property_group
    @property_group = PropertyGroup.new(params[:property_group])
    if @property_group.save
      flash[:notice] = "Successfully created property_group and properties."
      redirect_to :action => :property_group
    else
      flash.now[:notice] = "Not Created"
      render :action => 'configuration/property_group/form',
      :layout => "standard"
    end
  end

  def update_property_group
    params[:property_group][:existing_property_attributes] ||= {}
    @property_group = PropertyGroup.find(params[:id])
    if @property_group.update_attributes(params[:property_group])
      flash[:notice] = "Successfully updated property_group and properties."
      redirect_to :action => :property_group
    else
      flash.now[:notice] = "Not Updated"
      render :partial => "/configuration/wrapper/form",
	    :layout => "standard"
    end
  end

  #end property_group

  #pc_instance
  def pc_instance
    render :partial => 'configuration/pc_instance/form',
      :layout => 'standard',
      :locals => { :pc_instance => PcInstance.new, :processors => Processor.find(:all)}
  end

  def create_pc_instance
      pc_instance = PcInstance.new(params[:pc_instance])
    if pc_instance.save
      flash[:notice] = "Successfully created the processor instance"
      redirect_to :action => :pc_instance
    else
      flash.now[:notice] = "Not Created"
      render :partial => '/configuration/pc_instance/form',
	:layout => 'standard',
	:locals => { :pc_instance => pc_instance, :processors => Processor.find(:all) }
    end
  end
  def form_for_parameter
    pcinits = PcInit.find(:all,:conditions => "processor_id = #{params[:procid]}")
    render :partial => '/configuration/pc_instance/pc_parameter',
    :locals => { :pcinits => pcinits, :pc_parameter => PcParameter.new}
  end
end
