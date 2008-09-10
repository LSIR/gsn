class DataTypeController < ApplicationController
  active_scaffold :data_types do |config|
    a =  [:name, :description]
    config.columns = a
  end
end
