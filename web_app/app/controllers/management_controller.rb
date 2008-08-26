require 'rubygems'
require 'java'

class ManagementController < ApplicationController

  before_filter :authorize, :except => { :login => :logout }

  def index
    render :nothing=>true
  end
	
  def status
		
  end
	
  def structure
    #param[
    vs_list = Java::gsn.gui.util::VSensorIOUtil.new('virtual-sensors/', 'virtual-sensors/Disabled/')
    enabled_vs_files = vs_list.read_virtual_sensors
    render :nothing=>true
  end

  #
  #
  #

  def preferences
    @user = User.find session[:user_id]
  end

  def update_user
    @user = User.find session[:user_id]
    update_params = params[:user]
    if @user.update_attributes(update_params)
	flash.now[:success] = 'Successfully updated the user.'
    else
      flash.now[:failure] = 'Failed to update the user.'
    end
    render :controller => :management, :action => :preferences
  end

  #
  #
  #

  def logout
    session[:user_id] = nil
    redirect_to :controller => :gsn, :action => :home
  end

  def login
    session[:user_id] = nil
    if request.post?
      user = User.authenticate(params[:user][:email], params[:user][:password])
      if user
	session[:user_id] = user.id
	flash[:success] = 'Login successfully.'
	redirect_to :controller => :gsn, :action => :home
      else
	flash[:failure] = 'Invalid user/password combination.'
	redirect_to :controller => :gsn, :action => :home
      end
    else
      redirect_to :controller => :gsn, :action => :home
    end
  end

end
