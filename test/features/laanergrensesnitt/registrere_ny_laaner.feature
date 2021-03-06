# encoding: UTF-8
# language: no

Egenskap: Registrere låner
  Som en person
  For å kunne låne bøker i biblioteket
  Ønsker jeg å kunne registrere meg selv som låner

  Scenario: Ny voksenbruker registrerer seg i registreringsskjema
    Gitt at jeg er logget inn som adminbruker
    Og at det finnes en person som ikke er låner
    Og at jeg er i søkegrensesnittet
    Og jeg trykker på logg inn
    Og jeg trykker på registreringslenken
    Så skal jeg se registreringsskjemaet
    Når jeg legger inn mine personalia som "voksen"
    Og jeg trykker på knappen for å sjekke om jeg er registrert fra før
    Så får jeg vite at jeg ikke er registrert fra før
    Og jeg fyller inn resten av skjemaet
    Og jeg godtar lånerreglementet
    Og jeg trykker på registreringsknappen
    Så får jeg tilbakemelding om at registreringen er godkjent
    Og jeg har fått riktig lånerkategori som "voksen"
    Og jeg har fått et midlertidig brukernavn
    Og jeg kan søkes opp i systemet som låner

  Scenario: Ny barnebruker registrerer seg i registreringsskjema
    Gitt at jeg er logget inn som adminbruker
    Og at det finnes en person som ikke er låner
    Og at jeg er i søkegrensesnittet
    Og jeg trykker på logg inn
    Og jeg trykker på registreringslenken
    Så skal jeg se registreringsskjemaet
    Når jeg legger inn mine personalia som "barn"
    Og jeg trykker på knappen for å sjekke om jeg er registrert fra før
    Så får jeg vite at jeg ikke er registrert fra før
    Og jeg fyller inn resten av skjemaet
    Og jeg godtar lånerreglementet
    Og jeg trykker på registreringsknappen
    Så får jeg tilbakemelding om at registreringen er godkjent
    Og jeg har fått riktig lånerkategori som "barn"
    Og jeg har fått et midlertidig brukernavn
    Og jeg kan søkes opp i systemet som låner