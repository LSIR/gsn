class WrapperController < ApplicationController
  def new
    @wrapper = Wrapper.new
    @wrapper.wrapper_inits.build # Make a new wrapper init collection
    #    @wrapper.wrapper_inits[0][:data_type] = DataType.find(:first) # Add this type to the wrapper init collection.
    render :partial => "wrapper/form", :layout => "standard",:locals=>{:wrapper=>@wrapper}
  end

  def create
    @wrapper = Wrapper.new(params[:wrapper])
    if @wrapper.save
      flash[:notice] = "Successfully created the wrapper and wrapper_inits"
      #TODO redirect_to wrappers_path
      redirect_to :action => :index
    else
      render :partial => "wrapper/form", :layout => "standard",:locals=>{:wrapper=>@wrapper}
    end
  end

  def edit
    @wrapper = Wrapper.find(params[:id])
    render :partial => "wrapper/form", :layout => "standard",:locals=>{:wrapper=>@wrapper}
  end

  def update
    params[:wrapper][:existing_wrapper_init_attributes] ||= {}
    @wrapper = Wrapper.find(params[:id])
    if @wrapper.update_attributes(params[:wrapper])
      flash[:notice] = "Successfully updated wrapper and wrapper_inits."
       redirect_to :controller => :wrapper, :action => :edit
    else
      render :partial => "wrapper/form", :layout => "standard",:locals=>{:wrapper=>@wrapper}
    end
  end

  def index
    @wrappers = Wrapper.find(:all)
  end

end
