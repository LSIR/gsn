#http://svn.codehaus.org/jruby-contrib/trunk/rack/README.txt
# cd web_app; java -jar ../lib/core/jruby-complete.jar -S script/server

#p File.expand_path '.'
ENV['GEM_PATH']="#{File.expand_path('.')}/ruby_gems"

#p "#{File.expand_path('.')}/ruby_gems"

$LOAD_PATH << 'META-INF/jruby.home/lib/ruby/site_ruby/1.8'
$LOAD_PATH << "#{File.expand_path('.')}/ruby_gems"
#$LOAD_PATH << "#{File.expand_path '.'}/ruby_gems/lib"
#$LOAD_PATH << "lib/ruby/1.8"

require 'rubygems'
require 'mongrel'

Dir.chdir('./web_app')
#p File.expand_path '.' 
require "#{File.expand_path '.'}/config/boot"

require 'commands/server'
