// All backend URLs in one place (like urls.js in the LoanLinq front-end).
// Paths are relative to API_BASE (see client.js).

const endpoints = {
  auth: {
    getAuthToken: "/api/v1/auth/get_auth_token",
    accessAccount: "/api/v1/auth/access_account",
    me: "/api/v1/auth/me",
  },
  loans: {
    list: "/api/v1/loans",
    create: "/api/v1/loans",
    get: (id) => `/api/v1/loans/${id}`,
    amountDue: (id) => `/api/v1/loans/${id}/amount-due`,
    addCharge: (id) => `/api/v1/loans/${id}/charges`,
    waiveCharge: (id, chargeId) => `/api/v1/loans/${id}/charges/${chargeId}/waive`,
  },
  payments: {
    make: "/api/v1/payments",
    byLoan: (loanId) => `/api/v1/payments?loanId=${loanId}`,
    get: (id) => `/api/v1/payments/${id}`,
  },
  disbursements: "/api/v1/disbursements",
  nsfCases: "/api/v1/nsf-cases",
  borrowers: {
    list: "/api/v1/borrowers",
    create: "/api/v1/borrowers",
    me: "/api/v1/borrowers/me",
    myBankAccounts: "/api/v1/borrowers/me/bank-accounts",
    removeBankAccount: (id) => `/api/v1/borrowers/me/bank-accounts/${id}`,
  },
  lenders: {
    list: "/api/v1/lenders",
    create: "/api/v1/lenders",
    me: "/api/v1/lenders/me",
  },
  dev: {
    clock: "/api/v1/dev/clock",
    advanceClock: (days) => `/api/v1/dev/clock/advance?days=${days}`,
    resetClock: "/api/v1/dev/clock/reset",
    job: (name) => `/api/v1/dev/jobs/${name}`,
  },
};

export default endpoints;
