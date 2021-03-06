# encoding: UTF-8

require_relative 'catalinker_page.rb'

class WorkFlow < CatalinkerPage
  def visit
    retry_wait do
      @browser.goto catalinker(:workflow)
      Watir::Wait.until(BROWSER_WAIT_TIMEOUT) do
        @browser.elements(:class => 'prop-input').size > 1
      end # wait until dom-tree has been populated
    end
    self
  end

  def visit_landing_page_auth_maintenance
    retry_wait do
      @browser.goto catalinker(:landing_page_auth_maintenance)
      Watir::Wait.until(BROWSER_WAIT_TIMEOUT) do
        @browser.elements(:class => 'prop-input').size > 1
      end # wait until dom-tree has been populated
    end
    self
  end

  def next_step
    @browser.div(:class => 'grid-panel-selected').button(:class => 'next-step-button').click
  end

  def assert_selected_tab(name_of_visible_tag)
    Watir::Wait.until(BROWSER_WAIT_TIMEOUT) do
      @browser.ul(:id => 'workflow-tabs').a(:class => 'grid-tab-link-selected').text.eql? name_of_visible_tag
    end
  end

  def add_prop(domain, predicate, value, nr=0, skip_wait=false)
    super "#{domain}_#{predicate}", value, nr, skip_wait
  end

  def select_prop(domain, predicate, value, nr=0, skip_wait=false)
    @browser.inputs(:xpath => "//span[@data-automation-id='#{domain}_#{predicate}_#{nr}']//input[@type='search']")[0].click
    @browser.elements(:xpath => "//span[@class='select2-results']/ul/li[text()='#{value}']")[0].click
  end

  def get_available_select_choices(domain, predicate, nr=0)
    super "#{domain}_#{predicate}", nr
  end

  def get_available_select_choices_as_text(domain, predicate, nr=0)
    super "#{domain}_#{predicate}", nr
  end

  def get_link_to_work
    @browser.a(:data_automation_id => 'work_page_link')
  end

  def get_work_uri
    get_resource_uri 'work'
  end

  def get_publication_uri
    get_resource_uri 'publication'
  end

  def get_person_uri
    get_resource_uri 'person'
  end

  def get_resource_uri(type)
    attribute_name = "data-#{type.downcase}-uri"
    element = @browser.div(:data_automation_id => 'targetresources-uris')
#    Watir::Wait.until(BROWSER_WAIT_TIMEOUT) do
#      not element.attribute_value(attribute_name).empty?
#    end
    element.attribute_value(attribute_name)
  end

  def get_text_field_from_label(label)
    @browser.text_field(:xpath => "//*[./preceding-sibling::*[contains(concat(' ',normalize-space(@class),' '),' label ')][@data-uri-escaped-label='#{URI::escape(label)}']]//input")
  end

  def get_checkbox_from_label(label)
    @browser.checkbox(:xpath => "//*[./preceding-sibling::*[contains(concat(' ',normalize-space(@class),' '),' label ')][@data-uri-escaped-label='#{URI::escape(label)}']]//input[@type='checkbox']")
  end

  def get_select2_single_value_from_label(label)
    @browser.li(:xpath => "//*[./preceding-sibling::*[contains(concat(' ',normalize-space(@class),' '),' label ')][@data-uri-escaped-label='#{URI::escape(label)}']]//ul/li")
  end

  def get_low_range_text_field_from_label(label)
    @browser.text_field(:xpath => "//*[contains(concat(' ',normalize-space(@class),' '),' rangeStart ')]/*[./preceding-sibling::*[contains(concat(' ',normalize-space(@class),' '),' label ')][@data-uri-escaped-label='#{URI::escape(label)}']]//input")
  end

  def get_high_range_text_field_from_label(label)
    @browser.text_field(:xpath => "//*[contains(concat(' ',normalize-space(@class),' '),' rangeEnd ')][preceding-sibling::*/*[@data-uri-escaped-label='#{URI::escape(label)}']]//input")
  end

  def finish
    @browser.elements(:text => "Avslutt registrering av utgivelsen").first.click
  end
end
