puts "Starting GSN's HTTP Interface"

$LOAD_PATH << 'META-INF/jruby.home/lib/ruby/site_ruby/1.8'

Dir.chdir('./web_app')

RAILS_ROOT = File.expand_path '.'
ENV['GEM_PATH']="#{RAILS_ROOT}/../ruby_gems"
ENV['GEM_HOME']=ENV['GEM_PATH']
$LOAD_PATH << 'META-INF/jruby.home/lib/ruby/site_ruby/1.8'
$LOAD_PATH << ENV['GEM_PATH']

#p File.expand_path '.'

require "#{File.expand_path '.'}/config/boot"
require 'commands/server'
