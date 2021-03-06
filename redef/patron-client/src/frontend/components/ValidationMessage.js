import React, { PropTypes } from 'react'
import { defineMessages, FormattedMessage } from 'react-intl'

class ValidationMessage extends React.Component {
  render () {
    return messages[ this.props.message ]
      ? <FormattedMessage {...messages[ this.props.message ]} />
      : <FormattedMessage {...messages.genericFieldError} />
  }
}

ValidationMessage.propTypes = {
  message: PropTypes.string.isRequired
}

const messages = defineMessages({
  required: {
    id: 'ValidationMessage.required',
    description: 'Displayed below a field when not filled out',
    defaultMessage: 'Required'
  },
  invalidYear: {
    id: 'ValidationMessage.invalidYear',
    description: 'Displayed when the year is not valid',
    defaultMessage: 'Invalid year'
  },
  invalidMonth: {
    id: 'ValidationMessage.invalidMonth',
    description: 'Displayed when the month is not valid',
    defaultMessage: 'Invalid month'
  },
  invalidDay: {
    id: 'ValidationMessage.invalidDay',
    description: 'Displayed when the day is not valid',
    defaultMessage: 'Invalid day'
  },
  invalidEmail: {
    id: 'ValidationMessage.invalidEmail',
    description: 'Displayed when the email is not valid',
    defaultMessage: 'Invalid email address'
  },
  invalidPin: {
    id: 'ValidationMessage.invalidPin',
    description: 'Displayed when the PIN-code is not valid',
    defaultMessage: 'PIN-code must be exactly 4 digits'
  },
  pinsMustBeEqual: {
    id: 'ValidationMessage.pinsMustBeEqual',
    description: 'Displayed when the pin and repeat pin is not equal',
    defaultMessage: 'PINs must be equal'
  },
  termsMustBeAccepted: {
    id: 'ValidationMessage.termsMustBeAccepted',
    description: 'Displayed when Terms checkbox is not checked',
    defaultMessage: 'Terms and Conditions must be accepted'
  },
  illegalCharacters: {
    id: 'ValidationMessage.illegalCharacters',
    description: 'Displayed when the input contains illegal characters',
    defaultMessage: 'Illegal characters'
  },
  genericFieldError: {
    id: 'ValidationMessage.genericFieldError',
    description: 'Displayed when no other errors match',
    defaultMessage: 'Field input generated validation error'
  }
})

export default ValidationMessage
