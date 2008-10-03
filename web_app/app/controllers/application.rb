# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time

  # See ActionController::RequestForgeryProtection for details
  # Uncomment the :secret if you're not using the cookie session store
  protect_from_forgery # :secret => 'c629a41e0a970deafda4334cce3a45bf'
  
  # See ActionController::Base for details 
  # Uncomment this to filter the contents of submitted sensitive data parameters
  # from your application log (in this case, all fields with names like "password"). 
  # filter_parameter_logging :password
  layout "standard"
  
   CONTAINER_CONFIG = Java::gsn.Main::loadContainerConfig("#{RAILS_ROOT}/../conf/gsn.xml")

  #private:

  	vs_list = Java::gsn.gui.util::VSensorIOUtil.new(Java::gsn.Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY, 'virtual-sensors/Disabled/')
  	enabled_vs_files = vs_list.read_virtual_sensors
	disabled_vs_files = vs_list.read_disabled_virtual_sensors

  #public:
	VS_ENABLED = Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(enabled_vs_files)
	VS_NAME_TO_CONFIG = {}
	VS_ENABLED.each {|k| VS_NAME_TO_CONFIG[k[1].name.chomp.upcase] = k[1] }
	VS_DISABLED =  Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(disabled_vs_files)
	STORAGE_MANAGER = Java::gsn.storage::StorageManager.getInstance()
	STORAGE_MANAGER.init(CONTAINER_CONFIG.jdbc_driver, CONTAINER_CONFIG.jdbc_username, CONTAINER_CONFIG.jdbc_password, CONTAINER_CONFIG.jdbc_url)
	CONTAINER_INFO_HANDLER = Java::gsn.http::ContainerInfoHandler.new
  
end
