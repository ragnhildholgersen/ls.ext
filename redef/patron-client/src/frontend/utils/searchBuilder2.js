import Constants from '../constants/Constants'

export function parseFilters (locationQuery) {
  let filters = []
  Object.keys(locationQuery).forEach(key => {
    if (key.startsWith('filter_')) {
      let aggregation = key.split('_')[ 1 ]
      let bucket = locationQuery[ key ]
      filters.push({ aggregation: aggregation, bucket: bucket })
    }
  })
  return filters
}

export function browseQuery (locationQuery) {
  let browse = locationQuery.browse
  let query = locationQuery.query
  let filters = parseFilters(locationQuery)

  let elasticSearchQuery = initQuery(browse, query)
  let musts = {}
  let nestedMusts = {}

  filters.forEach(filter => {
    let path = getPath(filter.aggregation)
    if (musts[ path ]) {
      if (nestedMusts[ filter.aggregation ]) {
        nestedMusts[ filter.aggregation ].terms[ filter.aggregation ].push(filter.bucket)
      } else {
        let nestedMust = { terms: {} }
        nestedMust.terms[ filter.aggregation ] = [ filter.bucket ]
        musts[ path ].nested.query.bool.must.push(nestedMust)
        nestedMusts[ filter.aggregation ] = nestedMust
      }
    } else {
      let must = createMust(path)
      must.nested.query.bool.must[ 0 ].terms[ filter.aggregation ] = [ filter.bucket ]
      nestedMusts[ filter.aggregation ] = must.nested.query.bool.must[ 0 ]
      musts[ path ] = must
    }
  })

  Object.keys(musts).forEach(aggregation => {
    elasticSearchQuery.query.filtered.filter.bool.must.push(musts[ aggregation ])
  })

  elasticSearchQuery.size = Constants.searchQuerySize
  elasticSearchQuery.aggregations = { all: { global: {}, aggregations: {} } }

  Constants.filterableFields.forEach(field => {
    let aggregations = {
      filter: {
        and: [ elasticSearchQuery.query.filtered.query ]
      },
      aggregations: {
        [ field ]: {
          nested: {
            path: getPath(field)
          },
          aggregations: {
            [ field ]: {
              terms: {
                field: field
              }
            }
          }
        }
      }
    }

    Object.keys(musts).forEach(path => {
      let must = createMust(path)
      let nestedMusts = musts[ path ].nested.query.bool.must
      must.nested.query.bool.must = nestedMusts.filter(nestedMust => { return !nestedMust.terms[ field ] })
      aggregations.filter.and.push({ bool: { must: must } })
    })

    elasticSearchQuery.aggregations.all.aggregations[ field ] = aggregations
  })

  return elasticSearchQuery
}

function getPath (field) {
  return field.split('.').slice(0, -1).join('.')
}

function initQuery (browse, uri) {
  return {
    query: {
      filtered: {
        filter: {
          bool: {
            must: []
          }
        },
        query: {
          nested: {
            path: 'work.subjects',
            query: {
              multi_match: {
                query: uri,
                fields: [
                  browse
                ]
              }
            }
          }
        }
      }
    },
    highlight: {
      'pre_tags': [ '' ],
      'post_tags': [ '' ],
      fields: {
        'work.publications.mainTitle': {},
        'work.publications.partTitle': {},
        'work.contributors.agent.name': {}
      }
    }
  }
}

function createMust (path) {
  return {
    nested: {
      path: path,
      query: {
        bool: {
          must: [
            {
              terms: {}
            }
          ]
        }
      }
    }
  }
}
