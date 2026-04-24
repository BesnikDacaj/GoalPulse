import java.lang.reflect.Method;

public class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hash(String password) {
        try {
            Class<?> bcrypt = Class.forName("org.mindrot.jbcrypt.BCrypt");
            Method gensalt = bcrypt.getMethod("gensalt", int.class);
            Method hashpw = bcrypt.getMethod("hashpw", String.class, String.class);
            return (String) hashpw.invoke(null, password, gensalt.invoke(null, 12));
        } catch (ReflectiveOperationException missingDependency) {
            throw new IllegalStateException("BCrypt dependency is not on the classpath. Use backend/pom.xml or the demo fallback in GoalPulseServer.Password.", missingDependency);
        }
    }

    public static boolean verify(String password, String hash) {
        try {
            Class<?> bcrypt = Class.forName("org.mindrot.jbcrypt.BCrypt");
            Method checkpw = bcrypt.getMethod("checkpw", String.class, String.class);
            return (Boolean) checkpw.invoke(null, password, hash);
        } catch (ReflectiveOperationException missingDependency) {
            throw new IllegalStateException("BCrypt dependency is not on the classpath. Use backend/pom.xml or the demo fallback in GoalPulseServer.Password.", missingDependency);
        }
    }
}
