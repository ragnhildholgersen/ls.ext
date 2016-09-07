/**
 * Created by Nikolai on 05/09/16.
 */
import React, { PropTypes } from 'react'
import QueryString from 'query-string'

import SearchFilterBoxItem from '../components/SearchFilterBoxItem'
import Constants from '../constants/Constants'

const getFilters = (query) => {
  var paramsToUse
  if (query.back) {
    const back = query.back
    const backQuery = back.split('?')[ 1 ]
    paramsToUse = QueryString.parse(backQuery)
  } else {
    paramsToUse = query
  }
  const filters = paramsToUse[ 'filter' ]
  return parseFilters(filters)
}

const parseFilters = (filters) => {
  var parsedFilters = []
  if (filters) {
    if (!Array.isArray(filters)) {
      filters = [ filters ]
    }
    filters.forEach((filter) => {
      const filterParts = filter.split('_')
      const filterType = filterParts[ 0 ]
      const filterValue = filterParts[ 1 ]
      const parsedFilter = {
        active: true,
        bucket: Constants.filterableFields[ filterType ].prefix + filterValue,
        id: filterType + '_' + filterValue
      }
      parsedFilters.push(parsedFilter)
    })
  }
  return parsedFilters
}

const SearchFilterBox = ({ toggleFilter, query, titleText }) => {
  var filterbox
  if (getFilters(query).length > 0) {
    filterbox = <div>
      <p>{titleText}</p>
      <ul style={{ padding: '0' }}>
        {
          getFilters(query).filter((filter) => filter.active).map((filter) => {
            return (<SearchFilterBoxItem key={filter.id} filter={filter} toggleFilter={toggleFilter} />)
          })
        }
      </ul>
    </div>
  }
  return (
    <div>{filterbox}</div>
  )
}

SearchFilterBox.propTypes = {
  toggleFilter: PropTypes.func.isRequired,
  query: PropTypes.object.isRequired,
  titleText: PropTypes.string.isRequired
}

export default SearchFilterBox
