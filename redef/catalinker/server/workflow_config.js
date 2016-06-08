var _ = require('underscore')
function manintenanceInputs (label, type) {
  return {
    // this is an input type used to search for a main resource, e.g. Work. The rendered input field
    // will not be tied to a particular subject and predicate
    searchMainResource: {
      label: label,
      indexType: type
    },
    // this is used to control how the search result in the support panel behaves
    widgetOptions: {
      // make it possible to create a work resource if necessary,
      enableCreateNewResource: {
        formRefs: [ {
          formId: 'create-' + type + '-form',
          targetType: type
        } ]
      },
      enableEditResource: {
        formRefs: [ {
          formId: 'create-' + type + '-form',
          targetType: type
        } ]
      },
      enableInPlaceEditing: true
    }
  }
}

module.exports = (app) => {
  app.get('/config', function (request, response) {
    var config =
    {
      kohaOpacUri: (process.env.KOHA_OPAC_PORT || 'http://192.168.50.12:8080').replace(/^tcp:\//, 'http:/'),
      kohaIntraUri: (process.env.KOHA_INTRA_PORT || 'http://192.168.50.12:8081').replace(/^tcp:\//, 'http:/'),
      ontologyUri: '/services/ontology',
      resourceApiUri: '/services/',
      inputForms: [
        {
          id: 'create-person-form',
          rdfType: 'Person',
          labelForCreateButton: 'Opprett ny person',
          inputs: [
            {
              rdfProperty: 'name',
              displayValueSource: true,
              // after resource is created, the value entered
              // in input marked with this is used to populate displayValue of the parent input
              preFillFromSearchField: true,
              type: 'input-string'
            },
            {
              rdfProperty: 'birthYear'
            },
            {
              rdfProperty: 'deathYear'
            },
            {
              rdfProperty: 'nationality'
            },
            {
              rdfProperty: 'gender'
            },
            {
              rdfProperty: 'specification',
              type: 'input-string-large'
            }
          ]
        },
        {
          id: 'create-subject-form',
          labelForCreateButton: 'Opprett nytt generelt emne',
          rdfType: 'Subject',
          inputs: [
            {
              rdfProperty: 'prefLabel',
              displayValueSource: true,
              // after resource is created, the value entered
              // in input marked with this is used to populate displayValue of the parent input
              preFillFromSearchField: true
            },
            {
              rdfProperty: 'specification',
              type: 'input-string-large'
            }
          ]
        },
        {
          id: 'create-genre-form',
          labelForCreateButton: 'Opprett ny sjanger',
          rdfType: 'Genre',
          inputs: [
            {
              rdfProperty: 'name',
              displayValueSource: true,
              type: 'input-string',
              // after resource is created, the value entered
              // in input marked with this is used to populate displayValue of the parent input
              preFillFromSearchField: true
            },
            {
              rdfProperty: 'specification',
              type: 'input-string-large'
            }
          ]
        },
        {
          id: 'create-publisher-form',
          labelForCreateButton: 'Opprett ny utgiver',
          rdfType: 'Publisher',
          inputs: [
            {
              rdfProperty: 'name',
              displayValueSource: true,
              type: 'input-string',
              // after resource is created, the value entered
              // in input marked with this is used to populate displayValue of the parent input
              preFillFromSearchField: true
            }
          ]
        },
        {
          id: 'create-work-form',
          labelForCreateButton: 'Opprett nytt verk',
          rdfType: 'Work',
          inputs: [
            {
              label: 'Hovedtittel',
              rdfProperty: 'mainTitle',
              type: 'input-string',
              // input type must be defined explicitly, otherwise it will inherit from the search field above
              preFillFromSearchField: true // value of this field should be copied from the search field above
            },
            {
              label: 'Undertittel',
              type: 'input-string', // input type must be defined explicitly, otherwise it will inherit from the search field above
              rdfProperty: 'subtitle'
            }
          ]
        },
        {
          id: 'create-place-form',
          labelForCreateButton: 'Opprett nytt sted',
          rdfType: 'Place',
          inputs: [
            {
              label: 'Foretrukken betegnelse',
              rdfProperty: 'prefLabel',
              type: 'input-string', // input type must be defined explicitly, otherwise it will inherit from the search field above
              preFillFromSearchField: true // value of this field should be copied from the search field above
            },
            {
              label: 'Forklarende tilføyelse',
              rdfProperty: 'specification',
              type: 'input-string'
              // input type must be defined explicitly, otherwise it will inherit from the search field above
            }
          ]
        },
        {
          id: 'create-serial-form',
          labelForCreateButton: 'Opprett ny serie',
          rdfType: 'Serial',
          inputs: [
            {
              label: 'Navn',
              rdfProperty: 'name',
              type: 'input-string',
              displayValueSource: true,
              preFillFromSearchField: true
            }
          ]
        }
      ],
      tabs: [
        {
          id: 'confirm-person',
          rdfType: 'Work',
          label: 'Hovedinnførsel',
          inputs: [
            {
              // this is an input type used to search for a main resource, e.g. Work. The rendered input field
              // will not be tied to a particular subject and predicate
              searchForValueSuggestions: {
                label: 'ISBN',
                parameterName: 'isbn',
                automationId: 'searchValueSuggestions',
                showOnlyWhenMissingTargetUri: 'Work', // only show this search field if a work has not been loaded or created
                sources: [ 'bs', 'bb' ],
                preferredSource: {
                  id: 'bs',
                  name: 'Biblioteksentralen'
                }
              }
            },
            {
              label: 'Person (hovedinnførsel)',
              subInputs: { // input is a group of sub inputs, which are connected to resource as other ends of a blank node
                rdfProperty: 'contributor', // the rdf property of the resource
                range: 'Contribution', // this is the shorthand name of the type of the blank node
                inputs: [ // these are the actual sub inputs
                  {
                    label: 'Person',
                    required: true,
                    rdfProperty: 'agent',
                    indexTypes: 'person',
                    type: 'searchable-with-result-in-side-panel',
                    dependentResourceTypes: [ 'Work', 'Publication' ], // when the creator is changed, unload current work and publication
                    id: 'mainEntryPersonInput',
                    widgetOptions: {
                      showSelectWork: true, // show and enable select work radio button
                      enableCreateNewResource: {
                        formRefs: [ {
                          formId: 'create-person-form',
                          targetType: 'person'
                        } ],
                        useAfterCreation: false
                      }
                    }
                  },
                  {
                    label: 'Rolle',
                    rdfProperty: 'role',
                    required: true
                  }
                ]
              },
              isMainEntry: true,
              // blank nodes connected to this input is expected to have type deichman:MainEntry in addition to its own type (range)
              subjects: [ 'Work' ], // blank node can be attached to the the loaded resource of one of these types
              cssClassPrefix: 'additional-entries' // prefix of class names to identify a span surrounding this input or group of sub inputs.
              // actual names are <prefix>-non-editable and <prefix>-editable to enable alternative presentation when not editable
            },
            {
              // this is an input type used to search for a main resource, e.g. Work. The rendered input field
              // will not be tied to a particular subject and predicate
              searchMainResource: {
                label: 'Søk etter eksisterende verk',
                indexType: 'work',
                isRoot: true,
                automationId: 'searchWorkAsMainResource',
                showOnlyWhenMissingTargetUri: 'Work' // only show this search field if a work has not been loaded or created
              },
              suggestValueFrom: {
                domain: 'Work',
                predicate: '#mainTitle'
              },
              // this is used to control how the search result in the support panel behaves
              widgetOptions: {
                // make it possible to create a work resource if necessary,
                enableCreateNewResource: {
                  formRefs: [ {
                    formId: 'create-work-form',
                    targetType: 'work'
                  } ],
                  useAfterCreation: true
                },
                filter: {
                  inputRef: 'mainEntryPersonInput',
                  name: 'personAsMainEntryFilter'
                }
              }
            }
          ],
          nextStep: {
            buttonLabel: 'Neste steg: Beskrivelse',
            createNewResource: {
              type: 'Publication',
              prefillValuesFromResource: {
                'Work': [ 'mainTitle', 'subtitle', 'partTitle', 'partNumber', 'language' ]
              }
            }
          }
        },
        {
          id: 'describe-publication',
          rdfType: 'Publication',
          label: 'Beskriv utgivelse',
          inputs: [
            { rdfProperty: 'publicationOf', type: 'entity' },
            { rdfProperty: 'mainTitle' },
            { rdfProperty: 'subtitle' },
            { rdfProperty: 'partTitle' },
            { rdfProperty: 'partNumber' },
            { rdfProperty: 'edition' },
            { rdfProperty: 'publicationYear' },
            { rdfProperty: 'numberOfPages'},
            { rdfProperty: 'illustrativeMatter' },
            {
              rdfProperty: 'isbn',
              multiple: true,
              addAnotherLabel: 'Legg til et ISBN-nummer til'
            },
            { rdfProperty: 'binding' },
            { rdfProperty: 'language' },
            { rdfProperty: 'format', multiple: true },
            { rdfProperty: 'writingSystem', multiple: true },
            { rdfProperty: 'adaptationOfPublicationForParticularUserGroups', multiple: true },
            {
              rdfProperty: 'publishedBy',
              authority: true, // this indicates it is an authorized entity
              nameProperties: [ 'name' ], // these are property names used to label already connected entities
              indexTypes: 'publisher', // this is the name of the elasticsearch index type from which authorities are searched within
              indexDocumentFields: [ 'name' ], // these are indexed document JSON properties from which the labels f
              // or authoroty select list are concatenated
              type: 'searchable-with-result-in-side-panel',
              widgetOptions: {
                enableCreateNewResource: {
                  formRefs: [ {
                    formId: 'create-publisher-form',
                    targetType: 'publisher'
                  } ]
                }
              }
            },
            {
              rdfProperty: 'hasPlaceOfPublication',
              authority: true, // this indicates it is an authorized entity
              nameProperties: [ 'prefLabel', 'specification' ], // these are property names used to label already connected entities
              indexTypes: 'place', // this is the name of the elasticsearch index type from which authorities are searched within
              // the labels for authoroty select list are concatenated
              type: 'searchable-with-result-in-side-panel',
              widgetOptions: {
                enableCreateNewResource: {
                  formRefs: [ {
                    formId: 'create-place-form',
                    targetType: 'place'
                  } ]
                }
              }
            },
            {
              label: 'Serie',
              multiple: true,
              addAnotherLabel: 'Legg til en serie til',
              subjects: [ 'Publication' ],
              subInputs: {
                rdfProperty: 'inSerial',
                range: 'SerialIssue',
                inputs: [
                  {
                    label: 'Serie',
                    required: true,
                    rdfProperty: 'serial',
                    indexTypes: 'serial',
                    nameProperties: [ 'name' ],
                    type: 'searchable-with-result-in-side-panel',
                    widgetOptions: {
                      showSelectWork: false, // show and enable select work radio button
                      enableCreateNewResource: {
                        formRefs: [ {
                          formId: 'create-serial-form',
                          targetType: 'serial'
                        } ],
                        useAfterCreation: false
                      }
                    }
                  },
                  {
                    label: 'Nummer i serien',
                    rdfProperty: 'issue'
                  }
                ]
              }
            }
          ],
          nextStep: {
            buttonLabel: 'Neste steg: Verksopplysninger'
          }
        },
        {
          id: 'describe-work',
          rdfType: 'Work',
          label: 'Beskriv verk',
          inputs: [
            { rdfProperty: 'mainTitle', multiple: true },
            { rdfProperty: 'subtitle' },
            { rdfProperty: 'partTitle' },
            { rdfProperty: 'partNumber' },
            { rdfProperty: 'publicationYear' },
            { rdfProperty: 'language', multiple: true },
            { rdfProperty: 'literaryForm', multiple: true },
            { rdfProperty: 'audience', multiple: true },
            { rdfProperty: 'biography', multiple: true },
            { rdfProperty: 'adaptationOfWorkForParticularUserGroups', multiple: true }
          ],
          nextStep: {
            buttonLabel: 'Neste steg: Beskriv verket'
          }
        },
        {
          id: 'subjects',
          rdfType: 'Work',
          label: 'Emneopplysninger',
          inputs: [
            {
              rdfProperty: 'subject',
              multiple: true,
              addAnotherLabel: 'Legg til et emne til',
              type: 'searchable-with-result-in-side-panel',
              loadWorksAsSubjectOfItem: true,
              authority: true, // this indicates it is an authorized entity
              nameProperties: [ 'name', 'prefLabel', 'mainTitle', 'subTitle' ], // these are property names used to label already connected entities
              indexTypes: [ 'subject', 'person', 'work', 'place' ], // this is the name of the elasticsearch index type from which authorities are searched within
              widgetOptions: {
                selectIndexTypeLegend: 'Velg emnetype',
                enableCreateNewResource: {
                  formRefs: [ {
                    formId: 'create-subject-form',
                    targetType: 'subject' // these are matched against index types, hence lower case
                  },
                    {
                      formId: 'create-work-form',
                      targetType: 'work'
                    },
                    {
                      formId: 'create-person-form',
                      targetType: 'person'
                    },
                    {
                      formId: 'create-place-form',
                      targetType: 'place'
                    } ]
                }
              }
            },
            {
              rdfProperty: 'genre',
              multiple: true,
              addAnotherLabel: 'Legg til en sjanger til',
              type: 'searchable-with-result-in-side-panel',
              authority: true,
              nameProperties: [ 'name' ],
              indexTypes: [ 'genre' ],
              indexDocumentFields: [ 'name' ],
              widgetOptions: {
                enableCreateNewResource: {
                  formRefs: [ {
                    formId: 'create-genre-form',
                    targetType: 'genre'
                  } ]
                }
              }
            }
          ],
          nextStep: {
            buttonLabel: 'Neste steg: Biinnførsler'
          }
        },
        {
          // additional entries, such as translator, illustrator, composer etc
          id: 'confirm-addedentry',
          rdfType: 'Work',
          label: 'Biinnførsler',
          inputs: [
            {
              label: 'Biinnførsel',
              multiple: true, // can have many of these
              addAnotherLabel: 'Legg til ny biinnførsel',
              subInputs: { // input is a group of sub inputs, which are connected to resource as other ends of a blank node
                rdfProperty: 'contributor', // the rdf property of the resource
                range: 'Contribution', // this is the shorthand name of the type of the blank node
                inputs: [ // these are the actual sub inputs
                  {
                    label: 'Person',
                    required: true,
                    rdfProperty: 'agent',
                    indexTypes: 'person',
                    type: 'searchable-with-result-in-side-panel',
                    widgetOptions: {
                      showSelectWork: false, // show and enable select work radio button
                      enableCreateNewResource: {
                        formRefs: [ {
                          formId: 'create-person-form',
                          targetType: 'person'
                        } ],
                        useAfterCreation: false
                      }
                    }
                  },
                  {
                    label: 'Rolle',
                    required: true,
                    rdfProperty: 'role'
                  }
                ]
              },
              subjects: [ 'Work', 'Publication' ], // blank node can be attached to the the loaded resource of one of these types
              cssClassPrefix: 'additional-entries' // prefix of class names to identify a span surrounding this input or group of sub inputs.
              // actual names are <prefix>-non-editable and <prefix>-editable to enable alternative presentation when not editable
            }
          ],
          nextStep: {
            buttonLabel: 'Avslutt registrering av utgivelsen',
            restart: true
          }
        }

      ],
      authorityMaintenance: [
        {
          inputs: [
            manintenanceInputs('Personer', 'person'),
            manintenanceInputs('Emner', 'subject'),
            manintenanceInputs('Sjangre', 'genre'),
            manintenanceInputs('Serier', 'serial')
          ]
        }
      ],
      search: {
        person: {
          selectIndexLabel: 'Person',
          queryTerm: 'person.name',
          resultItemLabelProperties: [ 'name' ],
          resultItemDetailsLabelProperties: [ 'lifeSpan', 'nationality' ],
          itemHandler: 'personItemHandler'
        },
        subject: {
          selectIndexLabel: 'Generelt',
          queryTerm: 'subject.prefLabel',
          resultItemLabelProperties: [ 'prefLabel' ]
        },
        work: {
          structuredQuery: true,
          selectIndexLabel: 'Verk',
          queryTerm: 'work.mainTitle',
          resultItemLabelProperties: [ 'mainTitle', 'subTitle' ],
          resultItemDetailsLabelProperties: [ 'creator' ],
          itemHandler: 'workItemHandler'
        },
        genre: {
          selectIndexLabel: 'Sjanger',
          queryTerm: 'genre.name',
          resultItemLabelProperties: [ 'name' ]
        },
        publisher: {
          selectIndexLabel: 'Utgiver',
          queryTerm: 'publisher.name',
          resultItemLabelProperties: [ 'name' ]
        },
        place: {
          selectIndexLabel: 'Sted',
          queryTerm: 'place.prefLabel',
          resultItemLabelProperties: [ 'prefLabel', 'specification' ]
        },
        serial: {
          selectIndexLabel: 'Serie',
          queryTerm: 'serial.name',
          resultItemLabelProperties: [ 'name' ]
        }
      }
    }
    response.json(config)
  })
}
