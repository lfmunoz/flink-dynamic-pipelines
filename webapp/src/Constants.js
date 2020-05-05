
import * as logger from 'loglevel'
import { mapGetters } from "vuex";

let HOSTNAME = window.location.host
let SERVER_API_URL = `http://${HOSTNAME}/`
// XMLHttpRequest from a different domain cannot set cookie values for their own 
// domain unless withCredentials is set to true before making the request.
// https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/withCredentials
let WITH_CREDENTIALS = false

// Overwrite for development mode
if (process.env.NODE_ENV === 'development') {
    //SERVER_API_URL = `http://127.0.0.1:4010/`
    SERVER_API_URL = `http://127.0.0.1:8080/`
    WITH_CREDENTIALS = true
} 

export const NotifyType = {
    "GOOD": "GOOD",
    "BAD": "BAD",
    "TIP": "TIP",
    "BLANK": "BLANK"
  }

export function okResult(message) {
    return new Promise((resolve) => {
        resolve({ value: true, message: message })
    })
}

export function nokResult(message) {
    return new Promise((resolve, reject) => {
        reject({ value: false, message: message })
    })
}

export const curryMapGetters = args => (namespace, getters) =>
  Object.entries(mapGetters(namespace, getters)).reduce(
    (acc, [getter, fn]) => ({
      ...acc,
      [getter]: state =>
        fn.call(state)(...(Array.isArray(args) ? args : [args]))
    }),
    {}
);

// export curryMapGetters;
export function assert(condition, message) {
    if (!condition) {
        message = message || "Assertion failed";
        if (typeof Error !== "undefined") {
            throw new Error(message);
        }
        throw message; // Fallback
    }
}

let Constants = {

    // default router when on /dashboard. Used after login in
    dashboardDefault: '/dashboard/dashboardMain', 

    callHistoryPageSize: 20,
    logLevel: logger.levels.INFO,
    debugLevel: logger.levels.DEBUG,
    // logLevel: logger.levels.DEBUG,

    SERVER_API_URL: SERVER_API_URL,
    WITH_CREDENTIALS: WITH_CREDENTIALS,


} // end of object

// ________________________________________________________________________________
// EXPORT
// ________________________________________________________________________________
export default Constants;