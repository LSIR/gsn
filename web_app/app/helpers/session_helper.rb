module SessionHelper

  def logged_in?
    User.exists?(session[:user_id])
  end

  def logged_as_admin?
    logged_in? && is_admin?(User.find(session[:user_id]))
  end

  def is_admin?(user)
    user.type.to_s.eql? Admin.to_s
  end

end
