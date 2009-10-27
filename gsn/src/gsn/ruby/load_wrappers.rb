require 'java'

class RubyWrappers2 <Java::java.lang.Object
  def initialize 
  
  end
end

puts RubyWrappers2.java_class

#Loading all the wrappers in the wrappers directory.
Dir.glob("wrappers/*.rb").each { |filename| require filename } 