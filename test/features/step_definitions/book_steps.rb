# encoding: UTF-8

require 'net/http'

When(/^jeg legger til en materialtype "(.*?)" med kode "(.*?)"$/) do |name, code|
  @browser.goto intranet(:item_types)
  @browser.a(:id => "newitemtype").click
  form = @browser.form(:id => "itemtypeentry")
  form.text_field(:id => "itemtype").set code
  form.text_field(:id => "description").set name
  @context[:item_type_name] = name
  @context[:item_type_code] = code
  form.submit
end

Then(/^kan jeg se materialtypen i listen over materialtyper$/) do
  @browser.goto intranet(:item_types)
  table = @browser.table(:id => "table_item_type")
  table.should be_present
  table.text.should include(@context[:item_type_name])
  table.text.should include(@context[:item_type_code])
end

Gitt(/^at jeg har rettigheter til å katalogisere$/) do
  @http = Net::HTTP.new(host, 8081)
  res = @http.get("/cgi-bin/koha/svc/authentication?userid=#{SETTINGS['koha']['adminuser']}&password=#{SETTINGS['koha']['adminpass']}")
  res.body.should_not include("failed")
  @context[:svc_cookie] = res.response['set-cookie']
end

When(/^jeg legger inn "(.*?)" som ny bok$/) do |book|
  data = File.read("features/support/#{book}.marc21")
  headers = {
    'Cookie' => @context[:svc_cookie],
    'Content-Type' => 'text/xml'
  }
  res, data = @http.post("/cgi-bin/koha/svc/new_bib?items=1", data, headers)
  res.body.should include("<status>ok</status>")

  # Store the assigned id, so we can delete the book after:
  @context[:book_id] = res.body.match(/<biblionumber>(\d+)<\/biblionumber>/)[1]
end

Then(/^viser systemet at "(.*?)" er en bok som kan lånes ut$/) do |arg1|
  pending # express the regexp above with the code you wish you had
end