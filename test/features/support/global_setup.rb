require 'watir-webdriver'
require 'socket'

require_relative './site.rb'
require_relative './services/svc/preference.rb'

class GlobalSetup

  def initialize
    STDOUT.puts "SETUP: Populating global test settings"
    @growser = (Watir::Browser.new (ENV['BROWSER'] || "phantomjs").to_sym)
    Site.new(@growser).Login.visit.login(ENV['KOHA_ADMINUSER'], ENV['KOHA_ADMINPASS'])

    # Set language to English during testing, use global var to avvoid running before each scenario
    SVC::Preference.new(@growser).set("pref_language", "en")
    SVC::Preference.new(@growser).set("pref_opaclanguages", "en")

    # Disable item type icons due to slow loading
    SVC::Preference.new(@growser).set("pref_noItemTypeImages", "1")

    # Disable session restriction which breaks API usage
    SVC::Preference.new(@growser).set("pref_SessionRestrictionByIP", "0")

    # Enable extended patron attributes and messaging preferences, old ids, NL, etc.
    SVC::Preference.new(@growser).set("pref_ExtendedPatronAttributes", "1")
    SVC::Preference.new(@growser).set("pref_EnhancedMessagingPreferences", "1")

    # Check Previous Checkouts default yes but can be overridden
    # Needed for new patron category to work
    SVC::Preference.new(@growser).set("pref_CheckPrevCheckout", "softyes")

    sql = %Q{
INSERT IGNORE INTO branches  (branchcode, branchname)
VALUES ('hutl','Hovedbiblioteket');

INSERT IGNORE INTO itemtypes (itemtype, description)
VALUES ('B','Bok');

INSERT IGNORE INTO categories (categorycode, description, category_type, enrolmentperioddate)
VALUES('V','Voksen','A','2999-12-31');
}.gsub(/\s+/, " ").strip

    `mysql --default-character-set=utf8 -h koha_mysql -u#{ENV['KOHA_ADMINUSER']} -p#{ENV['KOHA_ADMINPASS']} koha_name -e "#{sql}"`
  end

  def teardown
    STDOUT.puts "TEARDOWN: Undoing global test settings"
    Site.new(@growser).Login.visit.login(ENV['KOHA_ADMINUSER'], ENV['KOHA_ADMINPASS'])

    # Reset to Norwegian after all tests are run
    SVC::Preference.new(@growser).set("pref_language", "nb-NO")
    SVC::Preference.new(@growser).set("pref_opaclanguages", "nb-NO")

    SVC::Preference.new(@growser).set("pref_noItemTypeImages", "1")
    @growser.close if @growser
  end
end

@testSuite = GlobalSetup.new

at_exit do
  @testSuite.teardown
end