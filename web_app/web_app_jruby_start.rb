$LOAD_PATH << 'META-INF/jruby.home/lib/ruby/site_ruby/1.8'

Dir.chdir('./web_app')
#p File.expand_path '.' 
require "#{File.expand_path '.'}/config/boot"
require 'commands/server'
