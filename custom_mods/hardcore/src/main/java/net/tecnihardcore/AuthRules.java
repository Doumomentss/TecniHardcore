package net.tecnihardcore;

public final class AuthRules {
    // Playit traffic can arrive from 127.29.x.x. Never trust all of 127/8.
    public static boolean localRegistration(String address) {
        return "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address);
    }

    public static String registrationError(String[] args) {
        if(args.length!=3)return "Formato incorrecto: /register CLAVE CLAVE. Escribe la misma contraseña dos veces, sin espacios dentro de ella.";
        if(args[1].length()<12)return "Tu contraseña tiene "+args[1].length()+" caracteres; necesita al menos 12. No se ha registrado la cuenta.";
        if(args[1].length()>128)return "Tu contraseña supera el máximo de 128 caracteres. No se ha registrado la cuenta.";
        if(!args[1].equals(args[2]))return "Las dos contraseñas no coinciden. Repite exactamente la misma contraseña después de /register.";
        return null;
    }
}
