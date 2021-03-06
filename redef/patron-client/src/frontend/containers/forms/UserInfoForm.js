import React, { PropTypes } from 'react'
import { bindActionCreators } from 'redux'
import { reduxForm } from 'redux-form'
import { injectIntl, intlShape, defineMessages, FormattedMessage } from 'react-intl'

import * as ParameterActions from '../../actions/ParameterActions'
import * as ProfileActions from '../../actions/ProfileActions'
import * as LoginActions from '../../actions/LoginActions'
import validator from '../../../common/validation/validator'
import fields from '../../../common/forms/userInfoForm'
import ValidationMessage from '../../components/ValidationMessage'
import asyncValidate from '../../utils/asyncValidate'
import domOnlyProps from '../../utils/domOnlyProps'

class UserInfoForm extends React.Component {
  constructor (props) {
    super(props)
    this.handleSubmit = this.handleSubmit.bind(this)
    this.handleChangeClick = this.handleChangeClick.bind(this)
  }

  handleSubmit () {
    this.props.loginActions.requireLoginBeforeAction(
      ProfileActions.postProfileInfo(ParameterActions.toggleParameter('edit'))
    )
  }

  handleChangeClick (event) {
    event.preventDefault()
    this.props.loginActions.requireLoginBeforeAction(ParameterActions.toggleParameter('edit'))
  }

  getValidator (field) {
    if (field.touched && field.error) {
      return <div style={{ color: 'red' }}><ValidationMessage message={field.error} /></div>
    }
  }

  render () {
    const { fields: { address, zipcode, city, country, mobile, email } } = this.props
    return (
      <form name="change-user-details" id="change-user-details">
        <div className="address col">

          <h2><FormattedMessage {...messages.address} /></h2>
          <address typeof="schema:PostalAddress">
              <span property="schema:streetAddress">
                <label htmlFor="streetaddress"><FormattedMessage {...messages.address} /></label>
                <input data-automation-id="UserInfoForm_address" id="streetaddress" type="text"
                       placeholder={this.props.intl.formatMessage(messages.address)} {...domOnlyProps(address)} />
                {this.getValidator(address)}
              </span><br />

            <span property="schema:postalCode" className="display-inline">
                <h2><FormattedMessage {...messages.zipcode} /></h2>
                <label htmlFor="postal">Postnr.</label>
                <input data-automation-id="UserInfoForm_zipcode" id="postal" type="text"
                       placeholder={this.props.intl.formatMessage(messages.zipcode)} {...domOnlyProps(zipcode)} />
              {this.getValidator(zipcode)}
              </span>

            <span property="schema:addressLocality" className="display-inline">
                <h2><FormattedMessage {...messages.city} /></h2>
                <label htmlFor="city">Poststed</label>
                <input data-automation-id="UserInfoForm_city" id="city" type="text"
                       placeholder={this.props.intl.formatMessage(messages.city)} {...domOnlyProps(city)} />
              {this.getValidator(city)}
              </span><br />

            <span property="schema:addressCountry">
                <h2><FormattedMessage {...messages.country} /></h2>
                <label htmlFor="country">Land</label>
                <input data-automation-id="UserInfoForm_country" id="country" type="text"
                       placeholder={this.props.intl.formatMessage(messages.country)} {...domOnlyProps(country)} />
              {this.getValidator(country)}
              </span><br />
          </address>
        </div>

        <div className="col">
          <div className="cell-phone">
            <h2><FormattedMessage {...messages.mobile} /></h2>
            <label htmlFor="cellphone">Mobil</label>
            <input data-automation-id="UserInfoForm_mobile" id="cellphone" type="number"
                   placeholder={this.props.intl.formatMessage(messages.mobile)} {...domOnlyProps(mobile)} /></div>
          {this.getValidator(mobile)}

          <div className="phone">
            <h2>Telefon</h2>
            <label htmlFor="phone"><FormattedMessage {...messages.country} /></label>
            <input data-automation-id="UserInfoForm_mobile" id="phone" type="number"
                   placeholder={this.props.intl.formatMessage(messages.mobile)} {...domOnlyProps(mobile)} /></div>

          <div className="email">
            <h2><FormattedMessage {...messages.email} /></h2>
            <label htmlFor="email">E-post</label>
            <input data-automation-id="UserInfoForm_email" id="email" type="email"
                   placeholder={this.props.intl.formatMessage(messages.email)} {...domOnlyProps(email)} />
            {this.getValidator(email)}
          </div>
        </div>
      </form>
    )
  }
}

UserInfoForm.propTypes = {
  dispatch: PropTypes.func.isRequired,
  profileActions: PropTypes.object.isRequired,
  personalInformation: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
  handleSubmit: PropTypes.func.isRequired,
  parameterActions: PropTypes.object.isRequired,
  isRequestingPersonalInformation: PropTypes.bool.isRequired,
  loginActions: PropTypes.object.isRequired,
  personalInformationError: PropTypes.object,
  intl: intlShape.isRequired
}

const messages = defineMessages({
  address: {
    id: 'UserInfoForm.address',
    description: 'The label for the address',
    defaultMessage: 'Address'
  },
  zipcode: {
    id: 'UserInfoForm.zipcode',
    description: 'The label for the zip code',
    defaultMessage: 'Zip code'
  },
  city: {
    id: 'UserInfoForm.city',
    description: 'The label for the city',
    defaultMessage: 'City'
  },
  country: {
    id: 'UserInfoForm.country',
    description: 'The label for the country',
    defaultMessage: 'Country'
  },
  mobile: {
    id: 'UserInfoForm.mobile',
    description: 'The label for the mobile',
    defaultMessage: 'Mobile'
  },
  telephone: {
    id: 'UserInfoForm.telephone',
    description: 'The label for the telephone',
    defaultMessage: 'Telephone'
  },
  email: {
    id: 'UserInfoForm.email',
    description: 'The label for the email',
    defaultMessage: 'Email'
  },
  required: {
    id: 'UserInfoForm.required',
    description: 'Displayed below a field when not filled out',
    defaultMessage: 'Required'
  },
  invalidEmail: {
    id: 'UserInfoForm.invalidEmail',
    description: 'Displayed when the email is not valid',
    defaultMessage: 'Invalid email address'
  }
})

function mapStateToProps (state) {
  return {
    personalInformationError: state.profile.personalInformationError,
    isRequestingPersonalInformation: state.profile.isRequestingPersonalInformation,
    personalInformation: state.profile.personalInformation,
    initialValues: state.profile.personalInformation
  }
}

function mapDispatchToProps (dispatch) {
  return {
    dispatch: dispatch,
    profileActions: bindActionCreators(ProfileActions, dispatch),
    parameterActions: bindActionCreators(ParameterActions, dispatch),
    loginActions: bindActionCreators(LoginActions, dispatch)
  }
}

export default reduxForm(
  {
    form: 'userInfo',
    fields: Object.keys(fields),
    asyncValidate,
    asyncBlurFields: Object.keys(fields).filter(field => fields[ field ].asyncValidation),
    validate: validator(fields)
  },
  mapStateToProps,
  mapDispatchToProps
)(injectIntl(UserInfoForm))
