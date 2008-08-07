#http://svn.codehaus.org/jruby-contrib/trunk/rack/README.txt
# cd web_app; java -jar ../lib/core/jruby-complete.jar -S script/server

$LOAD_PATH << 'META-INF/jruby.home/lib/ruby/site_ruby/1.8'

require 'rubygems'
require 'mongrel'

#p File.expand_path '.'
Dir.chdir('./web_app')
#p File.expand_path '.' 
require "#{File.expand_path '.'}/config/boot"

require 'commands/server'