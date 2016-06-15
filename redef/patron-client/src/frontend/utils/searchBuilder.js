import Constants from '../constants/Constants'

export function parseFilters (locationQuery) {
  let filterBuckets = {}
  const filterableFields = Constants.filterableFields
  Object.keys(locationQuery).forEach(parameter => {
    if (parameter === 'filter') {
      const values = locationQuery[ parameter ] instanceof Array ? locationQuery[ parameter ] : [ locationQuery[ parameter ] ]
      values.forEach(value => {
        const split = value.split('_')
        const filterableField = filterableFields[ split[ 0 ] ]
        const aggregation = filterableField.name
        if (!filterBuckets[aggregation]) {
          filterBuckets[aggregation] = []
        }
        let val = filterableField.prefix + value.substring(`${split[ 0 ]}_`.length)
        filterBuckets[aggregation].push(val)
      })
    }
  })
  let filters = []
  Object.keys(filterBuckets).forEach(aggregation => {
    filters.push({ aggregation: aggregation, bucket: filterBuckets[aggregation] })
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
        bool: elasticSearchQuery.query.filtered.query.bool
      },
      aggs: {
        [fieldName]: {
          terms: {
            field: fieldName,
            size: 0
          }
        }
      }
    }

    Object.keys(musts).forEach(aggregation => {
      let must = musts[aggregation]
      if (aggregation !== fieldName) {
        elasticSearchQuery.aggs.facets.aggs[ fieldName ].filter.bool.must.push(must)
      }
    })
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
                  default_operator: 'and',
                  fields: ["publication.mainTitle", "publication.partTitle", "publication.subjects", "publication.contributors.agent.name"]
                }
              }
            ],
            must: [],
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
                  'publication.subjects': query
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
