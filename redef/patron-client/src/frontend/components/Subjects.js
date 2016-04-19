import React, { PropTypes } from 'react'
import { defineMessages, FormattedMessage } from 'react-intl'

export default React.createClass({
  propTypes: {
    subjects: PropTypes.array.isRequired
  },
  renderLabel (subject) {
    let label = subject.prefLabel
    if (subject.specification) {
      label += ` (${subject.specification})`
    }
    return label
  },
  render () {
    let subjects = this.props.subjects.filter(subject => subject.prefLabel).map(subject => this.renderLabel(subject))
    return (
      <p>
        <strong><FormattedMessage {...messages.subject} /> </strong>
        <span data-automation-id='work_subjects'>{subjects.join(', ')}</span>
      </p>
    )
  }
})

const messages = defineMessages({
  subject: {
    id: 'Subjects.subject',
    description: 'The text displayed to identify subjects',
    defaultMessage: 'Subject:'
  }
})
