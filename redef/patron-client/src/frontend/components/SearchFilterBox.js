/**
 * Created by Nikolai on 05/09/16.
 */
import React, {PropTypes} from 'react'
import QueryString from 'query-string'

import SearchFilterBoxItem from '../components/SearchFilterBoxItem'
import Constants from '../constants/Constants'


const getFilters = (query) => {
    var paramsToUse
    if(query.back) {
        const back = query.back
        paramsToUse = QueryString.parse(back.substring(back.split('search?')[0].length + 'search?'.length))
    }
    else {
        paramsToUse = query
    }
    const filters = paramsToUse['filter']
    return parseFilters(filters)
}

const parseFilters = (filters) => {
    var parsedFilters = []
    if(filters){
        if(!Array.isArray(filters)){
            filters = [filters]
        }
        filters.forEach((filter) => {
            const filterType = filter.split('_')[0]
            const startIndex = filterType.length + 1
            const filterValue = filter.substring(startIndex, filter.length);
            const parsedFilter = {
                active: true,
                bucket: Constants.filterableFields[filterType].prefix + filterValue,
                id: filterType + '_' + filterValue
            }
            parsedFilters.push(parsedFilter)
        })
    }
    return parsedFilters
}

const SearchFilterBox = ({toggleFilter, query}) => (
    <div>
        <ul style={{padding: "0"}}>
            {
                getFilters(query).filter((filter) => filter.active).map((filter) => {
                    return (<SearchFilterBoxItem key={filter.id} filter={filter} toggleFilter={toggleFilter}/>)
                })
            }
        </ul>
    </div>
)

SearchFilterBox.propTypes = {
    toggleFilter: PropTypes.func.isRequired,
    query: PropTypes.object.isRequired
}

export default SearchFilterBox