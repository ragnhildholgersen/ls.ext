# encoding: UTF-8

Given(/^at det finnes et verk$/) do
  step "leverer systemet en ny ID for det nye verket"
end

Given(/^at det finnes et eksemplar av en bok registrert i Koha/) do
  steps %Q{
    Gitt at jeg er logget inn som adminbruker
    Gitt at det finnes en avdeling
    Når jeg legger til en materialtype
  }
  book = SVC::Biblio.new(@browser,@context,@active).add
  @active[:book] = book
  @context[:biblio]     = book.biblionumber
  @cleanup.push( "bok #{book.biblionumber}" =>
    lambda do
      SVC::Biblio.new(@browser).delete(book)
    end
  )
end

Given(/^at jeg er i katalogiseringsgrensesnittet$/) do
  @site.Catalinker.visit
end

Given(/^at systemet har returnert en ny ID for det nye verket$/) do
  step "leverer systemet en ny ID for det nye verket"
end

Given(/^at det er en feil i systemet som behandler katalogisering$/) do
  pending # express the regexp above with the code you wish you had
end

Given(/^at verket har en tittel$/) do
  step "jeg kan legge til tittel for det nye verket"
  step "grensesnittet viser at tittelen er lagret"
end

When(/^jeg legger til en inn alternativ tittel på det nye verket$/) do
  predicate = "http://192.168.50.12:8005/ontology#name"
  @context[:alt_title] = generateRandomString
  @browser.div(:class => predicate).button.click
  input = @browser.text_field(:data_automation_id => predicate+"_1")
  input.set(@context[:alt_title])
  input.fire_event :blur
end

When(/^jeg legger til tittel for det nye verket$/) do
  step "jeg kan legge til tittel for det nye verket"
end

When(/^jeg vil legge til et nytt verk$/) do
  true
end

When(/^jeg er på sida til verket$/) do
  identifier = @context[:identifier].sub(services(:work).to_s + "/","")
  @site.PatronClient.visit(identifier)
end

When(/^jeg forsøker å registrere ett nytt verk$/) do
  pending # express the regexp above with the code you wish you had
end

Then(/^leverer systemet en ny ID for det nye verket$/) do
  @context[:identifier] = @site.Catalinker.get_id()
  @context[:identifier].should_not be_empty
end

Then(/^jeg kan legge til tittel for det nye verket$/) do
  @context[:title] = generateRandomString
  @site.Catalinker.add_prop("http://192.168.50.12:8005/ontology#name", @context[:title])
end

Then(/^grensesnittet viser at tittelen er lagret$/) do
  Watir::Wait.until { @browser.div(:id => /save-stat/).text === "alle endringer er lagret" }
end

Then(/^får jeg beskjed om at noe er feil$/) do
  pending # express the regexp above with the code you wish you had
end

Then(/^ser jeg informasjon om verkets tittel og utgivelsesår$/) do
  @browser.refresh
  @site.PatronClient.getTitle.should include(@context[:title])
  @site.PatronClient.getDate.should include(@context[:date])
end

Then(/^ser jeg en liste over eksemplarer knyttet til verket$/) do
  @browser.refresh
  @site.PatronClient.existsExemplar().should == true
end

Then(/^viser systemet at tittel på verket har blitt registrert$/) do
  step "grensesnittet viser at tittelen er lagret"
end

Then(/^verkets tittel vises på verks\-siden$/) do
  step "jeg er på sida til verket"
  @site.PatronClient.getTitle.should include(@context[:title])
end

Then(/^viser systemet at alternativ tittel på verket har blitt registrert$/) do
  step "grensesnittet viser at tittelen er lagret"
end

Then(/^verkets alternative tittel vises på verks\-siden$/) do
  step "jeg er på sida til verket"
  @browser.refresh
  @site.PatronClient.getTitle.should include(@context[:alt_title])
end
