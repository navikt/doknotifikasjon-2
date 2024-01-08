package no.nav.doknotifikasjon.config.cxf.cookies;

/**
 * Lagrer en <i>Cookie</i> i trådens minne.
 */
public class CookieStore {

    private static final ThreadLocal<Object> requestCookie = new ThreadLocal<>();

    public static void setCookie(Object cookie) {
        requestCookie.set(cookie);
    }

    public static Object getCookie() {
        return requestCookie.get();
    }

}
