import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest2 {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("admin", "$2a$10$8.Xo1J9M7j50/X5x/z2qDO2uLtoC5n6aXb9l/wH8S6/ZJ8Z0271W.");
        System.out.println("Does 'admin' match the hash? " + matches);
    }
}
