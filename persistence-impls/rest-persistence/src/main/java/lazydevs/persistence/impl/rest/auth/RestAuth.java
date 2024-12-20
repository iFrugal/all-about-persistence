package lazydevs.persistence.impl.rest.auth;

import lazydevs.persistence.impl.rest.reader.RestGeneralReader;

public interface RestAuth {//https://github.com/spring-tips/spring-security-5-oauth-client/blob/master/oauth2-client/src/main/java/com/example/oauth2client/Oauth2ClientApplication.java

    void authorize(RestGeneralReader.RestInstruction restInstruction);
}
