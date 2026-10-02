package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuthRulesTest {
    @Test void invalidRegistrationExplainsExactFailure() {
        assertTrue(AuthRules.registrationError(new String[]{"register","12345678","12345678"}).contains("8 caracteres"));
        assertTrue(AuthRules.registrationError(new String[]{"register","abcdefghijkl","mnopqrstuvwx"}).contains("no coinciden"));
        assertTrue(AuthRules.registrationError(new String[]{"register","a".repeat(129),"a".repeat(129)}).contains("máximo de 128"));
        assertTrue(AuthRules.registrationError(new String[]{"register","abcdefghijkl"}).contains("Formato incorrecto"));
        assertNull(AuthRules.registrationError(new String[]{"register","abcdefghijkl","abcdefghijkl"}));
        assertNull(AuthRules.registrationError(new String[]{"register","a".repeat(128),"a".repeat(128)}));
    }
    @Test void ownerCanRegisterThroughExactLocalhost() {
        assertTrue(AuthRules.localRegistration("127.0.0.1"));
        assertTrue(AuthRules.localRegistration("::1"));
        assertTrue(AuthRules.localRegistration("0:0:0:0:0:0:0:1"));
    }
    @Test void tunnelAndRemoteConnectionsCannotClaimReservedIdentity() {
        assertFalse(AuthRules.localRegistration("127.29.81.70"));
        assertFalse(AuthRules.localRegistration("127.0.0.2"));
        assertFalse(AuthRules.localRegistration("192.168.1.2"));
        assertFalse(AuthRules.localRegistration(null));
    }
}
