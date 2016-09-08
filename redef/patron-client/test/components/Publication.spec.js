/* eslint-env mocha */
import expect from 'expect'
import React from 'react'
import TestUtils from 'react-addons-test-utils'
import ReactDOM from 'react-dom'
import Publication from '../../src/frontend/components/Publication'
import { IntlProvider } from 'react-intl'

function setup (propOverrides) {
  const props = {
    expandSubResource: () => {},
    startReservation: () => {},
    publication: {
      id: 'test_id',
      mainTitle: 'test_maintitle',
      publicationYear: 'test_publicationYear',
      languages: [ 'test_language' ],
      formats: [ 'test_format' ],
      items: [],
      available: true
    },
    ...propOverrides
  }

  const messages = {
    'test_format': 'test_format_english',
    'test_language': 'test_language_english'
  }
  const output = TestUtils.renderIntoDocument(
    <IntlProvider locale="en" messages={messages}>
      <Publication {...props} />
    </IntlProvider>
  )

  return {
    props: props,
    output: output,
    node: ReactDOM.findDOMNode(output)
  }
}

describe('components', () => {
  describe('Publication', () => {
    it('should render the publication with title, year, language, format and item count', () => {
      const { node, props } = setup()
      expect(node.querySelector("[data-automation-id='publication_title']").textContent).toBe(props.publication.mainTitle)
      expect(node.querySelector("[data-automation-id='publication_year']").textContent).toBe(props.publication.publicationYear)
      expect(node.querySelector("[data-automation-id='publication_languages']").textContent).toBe(`${props.publication.languages[ 0 ]}_english`)
      expect(node.querySelector("[data-automation-id='publication_available']").textContent).toBe('Available')
      expect(node.querySelector("[data-automation-id='publication_formats']").textContent).toBe(`${props.publication.formats[ 0 ]}_english`)
    })

    it('should combine main title and part title as title', () => {
      const { node, props } = setup({
        publication: {
          mainTitle: 'test_maintitle',
          partTitle: 'test_parttitle',
          formats: [],
          languages: [],
          items: []
        }
      })
      expect(node.querySelector("[data-automation-id='publication_title']").textContent).toBe(props.publication.mainTitle + ' — ' + props.publication.partTitle)
    })
  })
})
