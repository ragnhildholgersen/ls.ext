# encoding: utf-8
require 'pp'

# TODO: Should pull report dir (if any) from cucumber command options
REPORT_DIR = 'report'
FileUtils.mkdir_p REPORT_DIR

def filenameify(string, sep = '-')
  filename = string.downcase
  filename.gsub!(/[^a-z0-9\-_]+/, sep)
  unless sep.nil? || sep.empty?
    re_sep = Regexp.escape(sep)
    filename.gsub!(/#{re_sep}{2,}/, sep)
    filename.gsub!(/^#{re_sep}|#{re_sep}$/, '')
  end
  filename
end

def add_screenshot(name)
  filename = "#{filenameify(name)}.png"
  screenshot = @browser.screenshot
  embed screenshot.base64, 'image/png', "Screenshot"
  screenshot.save "#{REPORT_DIR}/#{filename}" # Keep on disk, as well
end

# BEFORE HOOKS will run in the same order of which they are registered.

Before do
  @context = {
    :defaults => {
        :branch => {:code => "hutl", :desc => "Hovedutlånet"},
        :item_type => {:code => "B", :desc => "Bok" },
        :patron_category => {:code => "V", :desc => "Voksen" }
    }
  }
  @active = {} # Hash of active context objects (Book, Patron, Branch, etc.)
  @cleanup = [] # A stack of operations to be undone in reversed order after feature
end

Before do |scenario|
  @browser = @browser || (Watir::Browser.new (ENV['BROWSER'] || "phantomjs").to_sym)
  @browser.window.resize_to(1200, 1024) unless ENV['BROWSER']
  @site = @site || Site.new(@browser)
end

Before do |scenario|
  $random_migrate ||= false
  $random_migrate_branchcode ||= false

  if scenario.source_tag_names.include?('@random_migrate')
    step "at jeg er logget inn som adminbruker"
    unless $random_migrate
      $random_migrate_branchcode = generateRandomString
      step "at det er aktivert en standard sirkulasjonsregel"
      @site.Branches.visit.create(generateRandomString, $random_migrate_branchcode)

      migrator = RandomMigrate::Migrator.new("http://#{ENV['HOST']}:#{port(:services)}")
      $random_migrate = migrator.generate_quick_test_set($random_migrate_branchcode)
      $random_migrate_record_ids = migrator.get_record_ids
      # TODO: Randommigrated data should be properly cleaned up after tests are done
      #      @cleanup.push("removing randomigrated publications for migration #{@context[:random_migrate_id]}" =>
      #                    lambda do
      #                      @context[:record_ids].each {|id| SVC::Biblio.new(@browser).delete(id)}
      #                    end
      #      )
    end
    @context[:random_migrate_branchcode] = $random_migrate_branchcode
    @context[:random_migrate_id] = $random_migrate
    @context[:record_ids] = $random_migrate_record_ids
  end
end

#  AFTER HOOKS will run in the OPPOSITE order of which they are registered.

AfterStep('@check-for-errors') do |scenario|
  sleep 0.1
  if @browser.div(:id => "errors").exist?
    unless @browser.div(:id => "errors").ps.length == 0
      fail("The errors <div> contained one or more error <p>'s'")
    end
  end
end

def title_of(scenario)
  (defined? scenario.name) ? scenario.name : scenario.scenario_outline.name
end

After do |scenario|
  @browser.close if @browser
end

After do |scenario| # cleanup based on @cleanup - in reverse order
  STDOUT.puts "--------------- Context: #{title_of(scenario)} "
  STDOUT.puts @context.pretty_inspect

  last_cleanup_exception = nil

  if @browser && @browser.driver.manage.logs.get("browser").length > 0
    STDOUT.puts "--------------- Messages to browser console:"
    STDOUT.puts @browser.driver.manage.logs.get("browser").select { |l| !l.to_s.start_with?("INFO") }
  end

  STDOUT.puts "--------------- Cleanup: #{title_of(scenario)} "
  if @browser
    step "at jeg er logget inn som adminbruker" if @cleanup.length > 0 # TODO Only relevant for Koha-related cleanups
  end
  @cleanup.reverse.each do |hash|
    cleanup_desc = " cleanup '#{hash.keys.first}'"
    cleanup_func = hash.values.first
    begin
      cleanup_func.call
      STDOUT.puts "#{cleanup_desc} completed"
    rescue Exception => e
      last_cleanup_exception = e
      STDOUT.puts "#{cleanup_desc} failed: #{e}"
      e.backtrace.each_with_index { |line, i| STDOUT.puts("  #{line}") if i < 3 }
      STDOUT.puts "--------------- Active context upon failure: #{cleanup_desc} "
      STDOUT.puts "#{@active.pretty_inspect}"
      add_screenshot("#{cleanup_desc}")
    end
  end

  STDOUT.puts("Scenario failed: #{scenario.exception}") if scenario.failed?
  STDOUT.puts("Cleanup failed: #{last_cleanup_exception}") if last_cleanup_exception
  STDOUT.puts "======================================================================================== "
  STDOUT.flush
  raise Exception.new("Cleanup failed: #{last_cleanup_exception}") if !scenario.failed? && last_cleanup_exception
end

After do |scenario|
  if scenario.failed? && @browser
    add_screenshot(title_of(scenario))
    STDOUT.puts "--------------- Active context upon failure: #{title_of(scenario)} "
    STDOUT.puts "#{@active.pretty_inspect}"
  end
end

# `FAIL_FAST=1 cucumber` to stop on first failure
After do |scenario|
  Cucumber.wants_to_quit = ENV['FAIL_FAST'] && scenario.failed?
end