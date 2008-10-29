class DeploymentsController < ApplicationController

  skip_before_filter :login_required

  uses_tiny_mce :options =>{
      :theme => "advanced",
      :editor_selector => "mceEditor",
      :theme_advanced_toolbar_location => "top",
      :theme_advanced_toolbar_align => "left",
      :theme_advanced_buttons1 => "bold,italic,underline,strikethrough,separator,bullist,numlist,outdent,indent,undo,redo,link,unlink,separator,forecolor,backcolor,image,separator,justifyleft,justifycenter,justifyright,justifyfull,separator,sepeator,cut,copy,paste,charmap",
      :theme_advanced_buttons2 => "",
      :theme_advanced_buttons3 => "",
      :theme_advanced_resizing => true,
      :width => "95%"
      }
  
  def deployments
    @deployments = Deployment.find(:all, :conditions => { :private => false })
  end

  def index
    @deployments = Deployment.find(:all)
    render :action=>:show
  end

  def show
    @deployments = Deployment.find(params[:id])
  end

  def new
    # Not used
  end

  def create
   dep=Deployment.new({:name=>params[:deployment][:name],:description=>params[:deployment][:description]})
   dep.private=false
    dep.save!
    redirect_to :action=>:index
  end

  # Used for create and new
  def update
    puts '-----------------------------------------------------------------'
    dep = Deployment.find(params[:id])
    dep[params[:param]]=params[:value]
    dep.save
    redirect_to :action=>:index
  end

  def destroy
   dep = Deployment.find(params[:id])
   dep.destroy
   redirect_to :action=>:index
  end
  
end
