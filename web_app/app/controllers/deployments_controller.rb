class DeploymentsController < ApplicationController

  skip_before_filter :login_required

  def deployments
    @deployments = Deployment.find(:all, :conditions => { :private => false })
  end
end
