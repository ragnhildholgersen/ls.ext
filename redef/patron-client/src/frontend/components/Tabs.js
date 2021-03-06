import React, { PropTypes } from 'react'
import { matchPattern } from 'react-router/lib/PatternUtils'

import Tab from './Tab'

class Tabs extends React.Component {
  render () {
    return (
      <div className="row">
        <ul className={this.props.tabBarClass} data-automation-id="tabs">
          {this.props.tabList.map(tab => (
            <Tab key={tab.label}
                 tab={tab}
                 className={matchPattern(tab.path, this.props.currentPath) ? `${this.props.tabClass} ${this.props.tabActiveClass}` : this.props.tabClass}
                 push={this.props.push} />
          ))}
        </ul>
      </div>
    )
  }
}

Tabs.propTypes = {
  tabList: PropTypes.array.isRequired,
  push: PropTypes.func.isRequired,
  currentPath: PropTypes.string.isRequired,
  tabBarClass: PropTypes.string.isRequired,
  tabClass: PropTypes.string.isRequired,
  tabActiveClass: PropTypes.string.isRequired
}

Tabs.defaultProps = {
  tabBarClass: 'tab-bar',
  tabClass: 'tab-bar-tab',
  tabActiveClass: 'tab-bar-tab-active'
}

export default Tabs

