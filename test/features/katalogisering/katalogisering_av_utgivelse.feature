# encoding: UTF-8
# language: no

@redef
Egenskap: Katalogisering av utgivelse
  Som katalogisator
  For at brukere skal kunne finne rett utgivelse
  Ønsker jeg å registrere utgivelser med format og språk

  Scenario: Verk finnes - utgivelse finnes ikke
    Gitt at det finnes et verk
    Når jeg registrerer inn opplysninger om utgivelsen
    Og jeg knytter utgivelsen til verket
    Så vises opplysningene om utgivelsen på verkssiden

  @wip
  Scenario: Verk finnes - utgivelse finnes ikke (2)
    Gitt at det finnes et verk
    Når jeg vil katalogisere en utgivelse
    Så får utgivelsen tildelt en post-ID i Koba
    Og det vises en lenke til posten i Koha i katalogiseringsgrensesnittet

  @wip
  Scenario: Verk og utgivelse finnes — eksemplar finnes ikke
    Gitt at det finnes et verk
    Og at det finnes en utgivelse
    Når jeg legger til et eksemplar av utgivelsen
    Så vises eksemplaret på verkssiden

  @wip
  Scenario: Verk og utgivelse finnes ikke

  @wip
  Scenario: Utgivelse finnes
