# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time

  # See ActionController::RequestForgeryProtection for details
  # Uncomment the :secret if you're not using the cookie session store
  #protect_from_forgery # :secret => 'c629a41e0a970deafda4334cce3a45bf'
  
  # See ActionController::Base for details 
  # Uncomment this to filter the contents of submitted sensitive data parameters
  # from your application log (in this case, all fields with names like "password"). 
  # filter_parameter_logging :password
  layout "standard"

  before_filter :login_required

  def login_required
    unless session[:user_id]
      flash[:notice] = 'Please log in!'
      redirect_to :controller => :home, :action => :home
    end
  end

end
