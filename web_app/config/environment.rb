# Be sure to restart your server when you modify this file

# Uncomment below to force Rails into production mode when
# you don't control web/app server and can't set it the proper way
# ENV['RAILS_ENV'] ||= 'production'

# Specifies gem version of Rails to use when vendor/rails is not present
RAILS_GEM_VERSION = '2.1.1' unless defined? RAILS_GEM_VERSION

# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')

Rails::Initializer.run do |config|
  # Settings in config/environments/* take precedence over those specified here.
  # Application configuration should go into files in config/initializers
  # -- all .rb files in that directory are automatically loaded.
  # See Rails::Configuration for more options.

  # Skip frameworks you're not going to use. To use Rails without a database
  # you must remove the Active Record framework.
  # config.frameworks -= [ :active_record, :active_resource, :action_mailer ]

  # Specify gems that this application depends on. 
  # They can then be installed with "rake gems:install" on new installations.
  # config.gem "bj"
  # config.gem "hpricot", :version => '0.6', :source => "http://code.whytheluckystiff.net"
  # config.gem "aws-s3", :lib => "aws/s3"

  # Only load the plugins named here, in the order given. By default, all plugins 
  # in vendor/plugins are loaded in alphabetical order.
  # :all can be used as a placeholder for all plugins not explicitly named
  # config.plugins = [ :exception_notification, :ssl_requirement, :all ]

  # Add additional load paths for your own custom dirs
  # config.load_paths += %W( #{RAILS_ROOT}/extras )

  # Force all environments to use the same logger level
  # (by default production uses :info, the others :debug)
  # config.log_level = :debug

  # Make Time.zone default to the specified zone, and make Active Record store time values
  # in the database in UTC, and return them converted to the specified local zone.
  # Run "rake -D time" for a list of tasks for finding time zone names. Comment line to use default local time.
  config.time_zone = 'UTC'

  # Your secret key for verifying cookie session data integrity.
  # If you change this key, all old sessions will become invalid!
  # Make sure the secret is at least 30 characters and all random, 
  # no regular words or you'll be exposed to dictionary attacks.
  config.action_controller.session = {
    :session_key => '_gsn_interface_session',
    :secret      => '1c7fc855cb4c90efabb764bf2da186190b68814799e7a8ddfb3b61cc624827c38dd272dd70359b2ff5b03b59b4c31095207831b6eee758f40ea03e8c0f7fdb0f'
  }

  # Use the database for sessions instead of the cookie-based default,
  # which shouldn't be used to store highly confidential information
  # (create the session table with "rake db:sessions:create")
  # config.action_controller.session_store = :active_record_store

  # Use SQL instead of Active Record's schema dumper when creating the test database.
  # This is necessary if your schema can't be completely dumped by the schema dumper,
  # like if you have constraints or database-specific column types
  # config.active_record.schema_format = :sql

  # Activate observers that should always be running
  # config.active_record.observers = :cacher, :garbage_collector
end

require 'custom_validations'


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
