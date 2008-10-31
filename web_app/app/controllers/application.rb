# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time

  # See ActionController::RequestForgeryProtection for details
  # Uncomment the :secret if you're not using the cookie session store
  #protect_from_forgery # :secret => 'c629a41e0a970deafda4334cce3a45bf'
  
  # See ActionController::Base for details 
  # Uncomment this to filter the contents of submitted sensitive data parameters
  # from your application log (in this case, all fields with names like "password"). 
  # filter_parameter_logging :password
  layout "standard"

  before_filter :login_required

  def login_required
    unless session[:user_id]
      flash[:notice] = 'Please log in!'
      redirect_to :controller => :home, :action => :home
    end
  end

end

require 'singleton'
require 'java'

class JavaBeans

  include Singleton

  attr_accessor :vs_enabled, :vs_name_to_config, :vs_disabled, :storage_manager, :container_info_handler

  def initialize
    puts "Loading the GSN Java objects..."

    @container_config = Java::gsn.Main::loadContainerConfig("#{RAILS_ROOT}/../conf/gsn.xml")
    @vs_list = Java::gsn.gui.util::VSensorIOUtil.new(Java::gsn.Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY, 'virtual-sensors/Disabled/')
    @enabled_vs_files = @vs_list.read_virtual_sensors
    @disabled_vs_files = @vs_list.read_disabled_virtual_sensors

    @vs_enabled = Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(@enabled_vs_files)
    @vs_name_to_config = {}
    @vs_enabled.each {|k| @vs_name_to_config[k[1].name.chomp.upcase] = k[1] }
    @vs_disabled =  Java::gsn.gui.util::VSensorConfigUtil.getVSensorConfigs(@disabled_vs_files)
    @storage_manager = Java::gsn.storage::StorageManager.getInstance()
    @storage_manager.init(@container_config.jdbc_driver, @container_config.jdbc_username, @container_config.jdbc_password, @container_config.jdbc_url)
    @container_info_handler = Java::gsn.http::ContainerInfoHandler.new
  end

  def ruby_to_java_string_a(ruby_array)
    java_array = Java::java.lang.String[ruby_array.size]::new
    ruby_array.each_with_index { |a_string,i| java_array[i] = a_string }
    return java_array
  end

end
