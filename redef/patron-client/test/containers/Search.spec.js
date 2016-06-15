/* eslint-env mocha */
import expect from 'expect'
import React from 'react'
import TestUtils from 'react-addons-test-utils'
import { Search } from '../../src/frontend/containers/Search'
import Constants from '../../src/frontend/constants/Constants'
import ReactDOM from 'react-dom'
import { IntlProvider } from 'react-intl'

function setup (propOverrides) {
  const props = {
    searchActions: { search: expect.createSpy() },
    searchResults: [],
    isSearching: false,
    dispatch: () => {},
    searchError: false,
    filters: [],
    location: { query: {} },
    locationQuery: {},
    totalHits: 0,
    totalHitsPublications: 0,
    searchFilterActions: {
      toggleFilter: () => {},
      toggleFilterVisibility: () => {},
      toggleAllFiltersVisibility: () => {},
      toggleCollapseFilter: () => {}
    },
    resourceActions: {
      fetchWorkResource: () => {}
    },
    resources: {},
    ...propOverrides
  }

  const output = TestUtils.renderIntoDocument(
    <IntlProvider locale='en'>
      <Search {...props} />
    </IntlProvider>
  )

  return {
    props: props,
    output: output,
    node: ReactDOM.findDOMNode(output)
  }
}

describe('containers', () => {
  describe('Search', () => {
    it('should search on mount when query is provided', () => {
      const { props } = setup({ locationQuery: { query: 'test_query' } })
      expect(props.searchActions.search).toHaveBeenCalled()
    })

    it('should not render pagination when few results', () => {
      const { node } = setup({ location: { query: { query: 'test' } }, totalHits: Constants.searchQuerySize })
      expect(node.querySelectorAll("[data-automation-id='search-results-pagination']").length).toBe(0)
    })

    it('should render pagination when many results', () => {
      const { node } = setup({
        location: { query: { query: 'test' } },
        totalHits: Constants.searchQuerySize + 1
      })
      expect(node.querySelectorAll("[data-automation-id='search-results-pagination']").length).toBe(1)
    })

    it('should render links to pages', () => {
      const { node } = setup({
        location: { query: { query: 'test' } },
        totalHits: Constants.searchQuerySize * 3
      })
      expect(node.querySelector("[data-automation-id='search-results-pagination']")
        .getElementsByClassName('pagination')[ 0 ].children.length).toBe(5) // including next and prev
    })
  })
})
