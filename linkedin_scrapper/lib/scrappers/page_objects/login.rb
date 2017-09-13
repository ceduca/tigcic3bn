class PageObjects::Login
  include PageObject

  URL = 'https://www.linkedin.com'

  text_field(:email, id: 'login-email')
  text_field(:password, id: 'login-password')
  form(:login_form, class: 'login-form')

  def go_to_page
    @browser.goto(URL)
    sleep(3)
  end

  def login!(username, password)
    email_element.send_keys(username)
    password_element.send_keys(password)

    login_form_element.submit
    sleep(4)
  end
end