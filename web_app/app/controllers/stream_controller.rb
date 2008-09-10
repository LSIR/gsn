class StreamController < ApplicationController
   active_scaffold :streams do |config|
    config.nested.add_link("Sources", [:sources])
  end
end
