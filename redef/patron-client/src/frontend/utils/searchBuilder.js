import Constants from '../constants/Constants'

export function parseFilters (locationQuery) {
  const filters = []
  const filterableFields = Constants.filterableFields
  Object.keys(locationQuery).forEach(parameter => {
    if (parameter === 'filter') {
      const values = locationQuery[ parameter ] instanceof Array ? locationQuery[ parameter ] : [ locationQuery[ parameter ] ]
      values.forEach(value => {
        const split = value.split('_')
        const filterableField = filterableFields[ split[ 0 ] ]
        const aggregation = filterableField.name
        const bucket = filterableField.prefix + value.substring(`${split[ 0 ]}_`.length)
        filters.push({ aggregation: aggregation, bucket: bucket })
      })
    }
  })
  return filters
}

export function filteredSearchQuery (locationQuery) {
  let query = locationQuery.query
  let filters = parseFilters(locationQuery)

  let elasticSearchQuery = initQuery(query)
  Object.keys(Constants.filterableFields).forEach(key => {
    const field = Constants.filterableFields[ key ]
    const fieldName = field.name
    elasticSearchQuery.aggs[ fieldName ] = {
      terms: {
        field: fieldName
      }
    }
  })

  return elasticSearchQuery
}

function getPath (field) {
  return field.split('.').slice(0, -1).join('.')
}

function initQuery (query) {
  return {
    query: {
      filtered: {
        filter: {
          bool: {
            must: []
          }
        },
        query: {
          bool: {
            filter: [
              {
                simple_query_string: {
                  query: query,
                  default_operator: 'and'
                }
              }
            ],
            should: [
              {
                nested: {
                  path: 'publication.contributors.agent',
                  query: {
                    multi_match: {
                      query: query,
                      fields: [ 'publication.contributors.agent.name^2' ]
                    }
                  }
                }
              },
              {
                multi_match: {
                  query: query,
                  fields: [ 'publication.mainTitle^2', 'publication.partTitle' ]
                }
              },
              {
                match: {
                  subject: query
                }
              }
            ]
          }
        }
      }
    },
    size: 0,
    aggs: {
      byWork: {
        terms: {
          field: 'publication.workUri',
          size: 100
        },
        aggs: {
          publications: {
            top_hits: {
              size: 1
            }
          }
        }
      },
      workCount : {
        cardinality: {
          field: 'publication.workUri'
        }
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
