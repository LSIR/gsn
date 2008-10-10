class DataController < ApplicationController

  def data
    render :partial => '/data/form',
      :layout => 'standard',
      :locals => { :data_configuration => DataConfiguration.new }
  end

  def deployment_selection
    deployment = (params[:deployment_id].nil? || params[:deployment_id] == '-1') ? Deployment.first : Deployment.find(params[:deployment_id])
    render :update do |page|
      page.insert_html :top, "data_selections", :partial => '/data/data_selection', :locals => { :deployment_increment => params[:deployment_increment], :deployment => deployment}
    end
  end

  def update_deployment_selection
    deployment = (params[:deployment_id].nil? || params[:deployment_id] == '-1') ? Deployment.first : Deployment.find(params[:deployment_id])
    render :update do |page|
      page.replace "deployment_increment_#{params[:deployment_increment]}",  :partial => '/data/data_selection', :locals => {:deployment_increment => (params[:deployment_increment].to_i + 100000).to_s, :deployment => deployment}
    end
  end

  def download_data
    from = params[:data_configuration][:from]
    to = params[:data_configuration][:to]

    puts "FROM: #{from} TO: #{to}"
  end

end
