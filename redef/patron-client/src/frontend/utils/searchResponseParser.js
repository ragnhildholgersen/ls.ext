import { relativeUri, getId } from './uriParser'
import Constants from '../constants/Constants'

export function processSearchResponse (response, locationQuery) {
  const processedResponse = {}
  if (response.error) {
    processedResponse.error = response.error
  } else {
    const searchResults = response.aggregations.byWork.buckets.map(element => {
      const result = {}

      result.workUri = element.key
      result.relativeUri = relativeUri(result.workUri)
      result.publication = element.publications.hits.hits[0]._source.publication
      result.publication.contributors = result.publication.contributors || []
      result.publication.contributors.forEach(contributor => {
        contributor.agent.relativeUri = relativeUri(contributor.agent.uri)
      })
      result.relativePublicationUri = `${result.relativeUri}${relativeUri(result.publication.uri)}`

      /*
      work.id = getId(work.uri)
      work.relativeUri = relativeUri(work.uri)

      work.contributors = work.contributors || []
      work.contributors.forEach(contributor => {
        contributor.agent.relativeUri = relativeUri(contributor.agent.uri)
      })

      work.subjects = work.subjects || []
      work.subjects.forEach(subject => {
        subject.searchQuery = `search?query=${subject.name}` // TODO: create and expose specialized query interface
      })

      work.publications = work.publications || []
      work.publications.forEach(publication => {
        publication.formats = publication.formats || []
        publication.languages = publication.languages || []
        if (publication.image) {
          // choose any random image from publication
          work.image = work.image || publication.image
        }
      })

      const chosenPublication = approximateBestTitle(work.publications, element.highlight)
      if (chosenPublication && chosenPublication.mainTitle) {
        work.mainTitle = chosenPublication.mainTitle
        work.partTitle = chosenPublication.partTitle
        work.relativePublicationUri = `${work.relativeUri}${relativeUri(chosenPublication.uri)}`
        if (chosenPublication.image) {
          // choose the image from pf present
          work.image = chosenPublication.image
        }
      }
      */
      return result
    })
    processedResponse.searchResults = searchResults
    processedResponse.totalHits = response.aggregations.workCount.value
    processedResponse.totalHitsPublications = response.hits.total
    processedResponse.filters = processAggregationsToFilters(response, locationQuery)
  }
  return processedResponse
}

export function processAggregationsToFilters (response, locationQuery) {
  const filters = []
  const filterParameters = locationQuery[ 'filter' ] instanceof Array ? locationQuery[ 'filter' ] : [ locationQuery[ 'filter' ] ]

  const all = response.aggregations
  Object.keys(Constants.filterableFields).forEach(fieldShortName => {
    const field = Constants.filterableFields[ fieldShortName ]
    const fieldName = field.name
    const aggregation = all[ fieldName ]
    if (aggregation) {
      aggregation.buckets.forEach(bucket => {
        const filterId = `${fieldShortName}_${bucket.key.substring(field.prefix.length)}`
        const filterParameter = filterParameters.find(filterParameter => filterParameter === filterId)
        const active = filterParameter !== undefined
        filters.push({ id: filterId, bucket: bucket.key, count: bucket.doc_count, active: active })
      })
    }
  })

  return filters
}