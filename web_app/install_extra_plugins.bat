# You need to have GIT installed on your machine. (Windows: http://code.google.com/p/msysgit/)
# You need to have Ruby installed on your machine.
# You need to have Ruby Gems installed on your machine. (http://rubyforge.org/frs/?group_id=126)
ruby ./script/plugin install git://github.com/kete/tiny_mce.git
ruby ./script/plugin install git://github.com/giraffesoft/attribute_fu.git
rake tiny_mce:install