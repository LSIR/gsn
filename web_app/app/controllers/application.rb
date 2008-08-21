require 'java'
# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time

  # See ActionController::RequestForgeryProtection for details
  # Uncomment the :secret if you're not using the cookie session store
  protect_from_forgery # :secret => '0f7697ec95cb7bf4a484f4e824022fe7'

  # Set the default layout
  layout 'standard'
  
  # See ActionController::Base for details 
  # Uncomment this to filter the contents of submitted sensitive data parameters
  # from your application log (in this case, all fields with names like "password"). 
  # filter_parameter_logging :password
  
#  CONTAINER_CONFIG = Java::gsn.Main::load_container_configuration

  #private:
 # 	vs_list = Java::gsn.gui.util::VSensorIOUtil.new('virtual-sensors/', 'virtual-sensors/Disabled/')
 # 	enabled_vs_files = vs_list.read_virtual_sensors
#	disabled_vs_files = vs_list.read_disabled_virtual_sensors
	
  #public:
#	VS_ENABLED = Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(enabled_vs_files)
#	VS_NAME_TO_CONFIG = {}
#	VS_ENABLED.each {|k| VS_NAME_TO_CONFIG[k[1].name.chomp.upcase] = k[1] }
#	VS_DISABLED =  Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(disabled_vs_files)
#	STORAGE_MANAGER = Java::gsn.storage::StorageManager.getInstance()
#	STORAGE_MANAGER.init(CONTAINER_CONFIG.jdbc_driver, CONTAINER_CONFIG.jdbc_username, CONTAINER_CONFIG.jdbc_password, CONTAINER_CONFIG.jdbc_url)

  # Check authorizations
  def authorize
    unless User.find_by_id(session[:user_id])
      flash[:warning] = 'You have to Login first.'
      redirect_to :controller => :gsn, :action => :home
    end
  end

end
