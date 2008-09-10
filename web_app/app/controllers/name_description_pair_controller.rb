class NameDescriptionPairController < ApplicationController
   active_scaffold :key_value_alls do |config|
      a = [:name, :description, :default_value, :optional]
      config.label = "Wrappers"
      config.columns = a
     # config.columns = a << :
  end
end
