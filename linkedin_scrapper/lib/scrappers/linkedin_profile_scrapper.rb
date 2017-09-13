class LinkedinProfileScrapper

  USERNAME = 'rli04141@posdz.com'
  PASSWORD = 'tigcic3n'

  def initialize
    @browser = Watir::Browser.new(:chrome)
  end

  def scrape_users_from_linkedin
    profile_page = PageObjects::Login.new(@browser)
    profile_page.go_to_page

    profile_page.login!(USERNAME, PASSWORD)

  ensure
    @browser.close if @browser
  end
end