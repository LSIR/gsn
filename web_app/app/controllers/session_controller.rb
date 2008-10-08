class SessionController < ApplicationController

  skip_before_filter :login_required

  def new
  end

  def create
    user = User.authenticate(params[:email], params[:password])
    if user
      session[:user_id] = user.id
      flash[:notice] = 'Successfully logged in!'
    else
      flash[:error] = 'Invalid email/password combination!'
    end
    redirect_to :controller => :home, :action => :home
  end

  def destroy
    reset_session
    flash[:notice] = "You've been logged out"
    redirect_to :controller => :home, :action => :home
  end
end
