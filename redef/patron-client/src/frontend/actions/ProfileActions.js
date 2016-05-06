import fetch from 'isomorphic-fetch'

import * as types from '../constants/ActionTypes'

export function requestProfileSettings () {
  return {
    type: types.REQUEST_PROFILE_SETTINGS
  }
}

export function profileSettingsFailure (error) {
  return {
    type: types.PROFILE_SETTINGS_FAILURE,
    payload: error,
    error: true
  }
}

export function receiveProfileSettings (settings) {
  return {
    type: types.RECEIVE_PROFILE_SETTINGS,
    payload: {
      settings: settings
    }
  }
}

export function fetchProfileSettings () {
  return (dispatch) => {
    let uri = '/api/v1/profile/settings'
    dispatch(requestProfileSettings())
    return fetch(uri, {
      method: 'GET',
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(json => dispatch(receiveProfileSettings(json)))
      .catch(error => dispatch(profileSettingsFailure(error)))
  }
}

export function requestProfileLoans () {
  return {
    type: types.REQUEST_PROFILE_LOANS
  }
}

export function profileLoansFailure (error) {
  return {
    type: types.PROFILE_LOANS_FAILURE,
    payload: error,
    error: true
  }
}

export function receiveProfileLoans (loans) {
  return {
    type: types.RECEIVE_PROFILE_LOANS,
    payload: {
      loans: loans
    }
  }
}

export function fetchProfileLoans () {
  return (dispatch) => {
    let uri = '/api/v1/profile/loans'
    dispatch(requestProfileLoans())
    return fetch(uri, {
      method: 'GET',
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(json => dispatch(receiveProfileLoans(json)))
      .catch(error => dispatch(profileLoansFailure(error)))
  }
}

export function requestProfileInfo () {
  return {
    type: types.REQUEST_PROFILE_INFO
  }
}

export function profileInfoFailure (error) {
  return {
    type: types.PROFILE_INFO_FAILURE,
    payload: error,
    error: true
  }
}

export function receiveProfileInfo (info) {
  return {
    type: types.RECEIVE_PROFILE_INFO,
    payload: {
      info: info
    }
  }
}

export function fetchProfileInfo () {
  return (dispatch) => {
    let uri = '/api/v1/profile/info'
    dispatch(requestProfileInfo())
    return fetch(uri, {
      method: 'GET',
      credentials: 'same-origin',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(json => dispatch(receiveProfileInfo(json)))
      .catch(error => dispatch(profileInfoFailure(error)))
  }
}

export function requestPostProfileInfo () {
  return {
    type: types.REQUEST_POST_PROFILE_INFO
  }
}

export function postProfileInfoFailure (error) {
  return {
    type: types.POST_PROFILE_INFO_FAILURE,
    payload: error,
    error: true
  }
}

export function postProfileInfoSuccess () {
  return {
    type: types.POST_PROFILE_INFO_SUCCESS
  }
}

export function postProfileInfo (profileInfo, successAction) {
  const url = '/api/v1/profile/info'
  return dispatch => {
    dispatch(requestPostProfileInfo())
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(profileInfo)
    })
      .then(response => {
        if (response.status === 200) {
          dispatch(postProfileInfoSuccess())
          dispatch(fetchProfileInfo())
          if (successAction) {
            dispatch(successAction)
          }
        } else {
          throw Error('Unexpected status code')
        }
      })
      .catch(error => dispatch(postProfileInfoFailure(error)))
  }
}

export function requestPostProfileSettings () {
  return {
    type: types.REQUEST_POST_PROFILE_SETTINGS
  }
}

export function postProfileSettingsFailure (error) {
  return {
    type: types.POST_PROFILE_SETTINGS_FAILURE,
    payload: error,
    error: true
  }
}

export function postProfileSettingsSuccess () {
  return {
    type: types.POST_PROFILE_SETTINGS_SUCCESS
  }
}

export function postProfileSettings (profileSettings, successAction) {
  const url = '/api/v1/profile/settings'
  return dispatch => {
    dispatch(requestPostProfileSettings())
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(profileSettings)
    })
      .then(response => {
        if (response.status === 200) {
          dispatch(postProfileSettingsSuccess())
          dispatch(fetchProfileSettings())
          if (successAction) {
            dispatch(successAction)
          }
        } else {
          throw Error('Unexpected status code')
        }
      })
      .catch(error => dispatch(postProfileSettingsFailure(error)))
  }
}