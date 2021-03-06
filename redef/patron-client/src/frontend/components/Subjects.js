import React, { PropTypes } from 'react'
import { defineMessages, FormattedMessage } from 'react-intl'
import { Link } from 'react-router'

class Subjects extends React.Component {
  renderLabel (subject) {
    let label = subject.prefLabel
    if (subject.specification) {
      label += ` (${subject.specification})`
    }
    return label
  }

  searchLink (subject) {
    return '/search?query=subject%3A' + subject
  }

  render () {
    const subjects = this.props.subjects.filter(subject => subject.prefLabel).map(subject => this.renderLabel(subject))
    return (
      <div>
        <h2><FormattedMessage {...messages.subjects} /></h2>
        <ul data-automation-id="work_subjects">
          {subjects.map(subject => <li key={subject}><Link to={this.searchLink(subject)} >{subject}</Link></li>)}
        </ul>
        <a className="patron-placeholder" href="#" alt="More subjects">Se flere emner</a>
      </div>
    )
  }
}

Subjects.propTypes = {
  subjects: PropTypes.array.isRequired
}

const messages = defineMessages({
  subjects: {
    id: 'Subjects.subjects',
    description: 'The text displayed to identify subjects',
    defaultMessage: 'Subjects:'
  }
})

export default Subjects
