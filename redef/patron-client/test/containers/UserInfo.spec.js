/* eslint-env mocha */
import expect from 'expect'
import React from 'react'
import TestUtils from 'react-addons-test-utils'
import UserInfo from '../../src/frontend/containers/UserInfo'
import ReactDOM from 'react-dom'
import { IntlProvider } from 'react-intl'
import { createStore } from 'redux'
import rootReducer from '../../src/frontend/reducers'
import { Provider } from 'react-redux'
import * as ProfileActions from '../../src/frontend/actions/ProfileActions'

function setup (propOverrides) {
  const props = {
    location: { query: {} },
    ...propOverrides
  }

  const profileInformation = {
    address: 'address',
    birthdate: 'birthdate',
    borrowerNumber: 'borrowerNumber',
    city: 'city',
    country: 'country',
    email: 'email',
    lastUpdated: 'lastUpdated',
    loanerCardIssued: 'loanerCardIssued',
    loanerCategory: 'loanerCategory',
    mobile: 'mobile',
    name: 'name',
    zipcode: 'zipcode'
  }

  const store = createStore(rootReducer)
  store.dispatch(ProfileActions.receiveProfileInfo(profileInformation))

  const output = TestUtils.renderIntoDocument(
    <Provider store={store}>
      <IntlProvider locale="en">
        <UserInfo {...props} />
      </IntlProvider>
    </Provider>
  )

  return {
    props: props,
    output: output,
    node: ReactDOM.findDOMNode(output),
    store: store
  }
}

describe('containers', () => {
  describe('UserInfo', () => {
    it('should display values from store', () => {
      const { node, store } = setup({ location: { query: {} } })
      const { personalInformation } = store.getState().profile
      Object.keys(personalInformation).forEach(key => {
        const value = personalInformation[ key ]
        expect(node.querySelector(`[data-automation-id='UserInfo_${key}']`).textContent).toEqual(value)
      })
    })
  })
})
