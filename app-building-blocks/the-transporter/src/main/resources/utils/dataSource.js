var BasicDataSource = Java.type('org.apache.commons.dbcp2.BasicDataSource');
var Integer = Java.type("java.lang.Integer");

function getDataSource(driverClassName, jdbcUrl, username, password){
    var basicDataSource = new BasicDataSource();
    basicDataSource.setDriverClassName(driverClassName);
    basicDataSource.setUrl(jdbcUrl);
    basicDataSource.setUsername(username);
    basicDataSource.setPassword(password);
    basicDataSource.setInitialSize(Integer.parseInt("1"));
    basicDataSource.setMaxIdle(Integer.parseInt("1"));
    basicDataSource.setMinIdle(Integer.parseInt("1"));
    return basicDataSource;
}
