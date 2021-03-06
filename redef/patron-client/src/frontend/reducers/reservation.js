import {
  REQUEST_RESERVE_PUBLICATION,
  RESERVE_PUBLICATION_FAILURE,
  RESERVE_PUBLICATION_SUCCESS,
  REQUEST_EXTEND_LOAN,
  EXTEND_LOAN_FAILURE,
  EXTEND_LOAN_SUCCESS,
  REQUEST_CANCEL_RESERVATION,
  CANCEL_RESERVATION_FAILURE,
  CANCEL_RESERVATION_SUCCESS
} from '../constants/ActionTypes'

const initialState = {
  isRequestingReservation: false,
  reservationError: false,
  isRequestingExtendLoan: false,
  extendLoanError: false,
  isRequestingCancelReservation: false,
  cancelReservationError: false
}

export default function reservation (state = initialState, action) {
  switch (action.type) {
    case REQUEST_RESERVE_PUBLICATION:
      return { ...state, isRequestingReservation: true, reservationError: false }
    case RESERVE_PUBLICATION_SUCCESS:
      return { ...state, isRequestingReservation: false, reservationError: false }
    case RESERVE_PUBLICATION_FAILURE:
      return { ...state, isRequestingReservation: false, reservationError: action.payload.error }
    case REQUEST_EXTEND_LOAN:
      return { ...state, isRequestingExtendLoan: true, extendLoanError: false }
    case EXTEND_LOAN_SUCCESS:
      return { ...state, isRequestingExtendLoan: false, extendLoanError: false }
    case EXTEND_LOAN_FAILURE:
      return { ...state, isRequestingExtendLoan: false, extendLoanError: action.payload.error }
    case REQUEST_CANCEL_RESERVATION:
      return { ...state, isRequestingCancelReservation: true, cancelReservationError: false }
    case CANCEL_RESERVATION_SUCCESS:
      return { ...state, isRequestingCancelReservation: false, cancelReservationError: false }
    case CANCEL_RESERVATION_FAILURE:
      return { ...state, isRequestingCancelReservation: false, cancelReservationError: action.payload.error }
    default:
      return state
  }
}
