import React, { PropTypes } from 'react'
import { Link } from 'react-router'
import { push } from 'react-router-redux'
import { injectIntl, intlShape, defineMessages, FormattedMessage } from 'react-intl'
import createPath from '../utils/createPath'

import SearchResultsText from './SearchResultsText'

const SearchHeader = React.createClass({
  propTypes: {
    locationQuery: PropTypes.object.isRequired,
    dispatch: PropTypes.func.isRequired,
    loadLanguage: PropTypes.func.isRequired,
    locale: PropTypes.string.isRequired,
    totalHits: PropTypes.number.isRequired,
    showLoginDialog: PropTypes.func.isRequired,
    isLoggedIn: PropTypes.bool.isRequired,
    logout: PropTypes.func.isRequired,
    intl: intlShape.isRequired
  },
  handleChangeLanguage (event) {
    this.props.loadLanguage(event.target.value)
  },
  search (event) {
    event.preventDefault()
    let url = createPath({ pathname: '/search', query: { query: this.searchFieldInput.value } })
    this.props.dispatch(push(url))
  },
  handleLoginClick (event) {
    event.preventDefault()
    this.props.showLoginDialog()
  },
  render () {
    return (
      <div>
        <header className='row'>
          <div className='container'>
            <div className='logo'>
              <Link to='/'>
                <img src='/images/logo.png' alt={this.props.intl.formatMessage({...messages.logoAlt})} />
              </Link>
            </div>
            <button type='button' className='btn-mobile'>
              <Link to='/'>
                <img src='/images/btn-mobile.svg' alt='3 black bars' />
              </Link>
            </button>
            <div className='primary-menu'>
              <ul>
                <li><Link to='/profile'><FormattedMessage {...messages.myProfile} /></Link></li>
                <li><FormattedMessage {...messages.myLoans} /></li>
                <li><FormattedMessage {...messages.more} /></li>
                <li><FormattedMessage {...messages.contactUs} /></li>
                {this.props.isLoggedIn
                  ? <li onClick={this.props.logout}><FormattedMessage { ...messages.logout } /></li>
                  : <li onClick={this.handleLoginClick}><FormattedMessage {...messages.logIn } />
                </li>}
              </ul>
            </div>
          </div>
        </header>
        <section className='search-container'>
          <div className='search-box'>
            <form onSubmit={this.search}>
              <input placeholder={this.props.intl.formatMessage({...messages.searchInputPlaceholder})}
                     type='search'
                     defaultValue={this.props.locationQuery.query || ''}
                     ref={e => this.searchFieldInput = e}
                     data-automation-id='search_input_field'
              />
              <button onClick={this.search} type='button' className='search-submit' data-automation-id='search_button'>
                <FormattedMessage {...messages.search} />
              </button>
            </form>
          </div>
        </section>
        <footer className='search-results-footer'>
          <div className='search-results-number'>
            <SearchResultsText totalHits={this.props.totalHits} locationQuery={this.props.locationQuery} />
          </div>
          <div className='search-sorting'>
            <p>Sorter treff på</p>
            <select>
              <option>Årstall</option>
            </select>
          </div>
        </footer>
      </div>
    )
  }
})

const messages = defineMessages({
  logoAlt: {
    id: 'SearchHeader.logoAlt',
    description: 'Alt text for the logo',
    defaultMessage: 'Black logo with text'
  },
  myProfile: {
    id: 'SearchHeader.myProfile',
    description: 'Label for the link to go to the user profile',
    defaultMessage: 'My profile'
  },
  myLoans: {
    id: 'SearchHeader.myLoans',
    description: 'Label for the link to go to the users loans',
    defaultMessage: 'My loans'
  },
  more: {
    id: 'SearchHeader.more',
    description: 'Label for the link to show more',
    defaultMessage: 'More'
  },
  contactUs: {
    id: 'SearchHeader.contactUs',
    description: 'Label for the link to go to the contact page',
    defaultMessage: 'Contact us'
  },
  english: {
    id: 'SearchHeader.english',
    description: 'Label for the English language choice',
    defaultMessage: 'English'
  },
  norwegian: {
    id: 'SearchHeader.norwegian',
    description: 'Label for the Norwegian language choice',
    defaultMessage: 'Norwegian'
  },
  searchInputPlaceholder: {
    id: 'SearchHeader.searchInputPlaceholder',
    description: 'Placeholder for the search field',
    defaultMessage: 'Search for something...'
  },
  search: {
    id: 'SearchHeader.search',
    description: 'Label on search button',
    defaultMessage: 'Search'
  },
  logout: {
    id: 'SearchHeader.logout',
    description: 'Shown when logged in',
    defaultMessage: 'Log out'
  },
  logIn: {
    id: 'SearchHeader.logIn',
    description: 'Shown when logged out',
    defaultMessage: 'Log in'
  }
})

export default injectIntl(SearchHeader)
