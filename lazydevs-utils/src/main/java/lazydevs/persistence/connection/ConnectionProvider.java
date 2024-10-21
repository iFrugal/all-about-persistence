package lazydevs.persistence.connection;

/**
 * @author Abhijeet Rai
 */
public interface ConnectionProvider<T> {
    T getConnection();
}
