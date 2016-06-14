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
        let bucket = filterableField.prefix + value.substring(`${split[ 0 ]}_`.length)
        if (!Array.isArray(bucket)) {
          bucket = [bucket]
        }
        filters.push({ aggregation: aggregation, bucket: bucket })
      })
    }
  })
  return filters
}

export function filteredSearchQuery (locationQuery) {
  const { query } = locationQuery
  const filters = parseFilters(locationQuery)

  const elasticSearchQuery = initQuery(query)
  let musts = {}
  filters.forEach(filter => {
    let aggregation = filter.aggregation
    let must = createMust(aggregation, filter.bucket)
    musts[ aggregation ] = must
  })

  Object.keys(musts).forEach(aggregation => {
    elasticSearchQuery.query.filtered.filter.bool.must.push(musts[ aggregation ])
  })

  Object.keys(Constants.filterableFields).forEach(key => {
    const field = Constants.filterableFields[ key ]
    const fieldName = field.name
    elasticSearchQuery.aggs.facets.aggs[ fieldName ] = {
      filter: {
        and: [ elasticSearchQuery.query.filtered.query ]
      },
      aggs: {
        [fieldName]: {
          terms: {
            field: fieldName
          }
        }
      }
    }
  })


  return elasticSearchQuery
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
      facets: {
        global: {},
        aggs: {}
      },
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
      workCount: {
        cardinality: {
          field: 'publication.workUri'
        }
      }
    }
  }
}

function createMust (field, terms) {
  return {
    terms: {
      [field]: terms
    }
  }
}
