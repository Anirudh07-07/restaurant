/**
 * Kaveri Restaurant — Immediate Administrative Route Guard
 * Executes synchronously in <head> before DOM parsing to prevent
 * unauthorized rendering, UI information disclosure, and flash of content.
 */
(function () {
    'use strict';
    try {
        var token = localStorage.getItem('fn_token');
        var userStr = localStorage.getItem('fn_user');
        var user = userStr ? JSON.parse(userStr) : null;
        if (!token || !user || user.role !== 'ROLE_ADMIN') {
            window.location.replace('/login.html?redirect=' + encodeURIComponent(window.location.pathname));
        }
    } catch (_) {
        window.location.replace('/login.html');
    }
})();
