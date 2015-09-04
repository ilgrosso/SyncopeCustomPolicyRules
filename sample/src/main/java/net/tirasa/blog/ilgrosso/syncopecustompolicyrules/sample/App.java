/*
 * Copyright (C) 2015 Tirasa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.blog.ilgrosso.syncopecustompolicyrules.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Response;
import net.tirasa.blog.ilgrosso.syncopecustompolicyrules.common.CustomAccountRuleConf;
import net.tirasa.blog.ilgrosso.syncopecustompolicyrules.common.CustomPasswordRuleConf;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.UserService;

public class App {

    private static final String ADMIN_UNAME = "admin";

    private static final String ADMIN_PWD = "password";

    private static final String ADDRESS = "http://localhost:9080/syncope/rest/";

    private static final SyncopeClientFactoryBean clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS);

    private static final SyncopeClient client = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

    private static final String ACCOUNT_REQUIRED_SUBSTRING = "tralallallero";

    private static AttrTO attrTO(final String schema, final String value) {
        AttrTO attr = new AttrTO();
        attr.setSchema(schema);
        attr.getValues().add(value);
        return attr;
    }

    private static UserTO getSampleTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername(email);

        userTO.getPlainAttrs().add(attrTO("fullname", email));
        userTO.getPlainAttrs().add(attrTO("firstname", email));
        userTO.getPlainAttrs().add(attrTO("surname", "surname"));
        userTO.getPlainAttrs().add(attrTO("type", "a type"));
        userTO.getPlainAttrs().add(attrTO("userId", email));
        userTO.getPlainAttrs().add(attrTO("email", email));
        userTO.getDerAttrs().add(attrTO("cn", null));
        userTO.getVirAttrs().add(attrTO("virtualdata", "virtualvalue"));

        userTO.setSecurityQuestion(1L);
        userTO.setSecurityAnswer(userTO.getPassword());

        return userTO;
    }

    private static <T> T getObject(final URI location, final Class<?> serviceClass, final Class<T> resultClass) {
        WebClient webClient = WebClient.fromClient(WebClient.client(client.getService(serviceClass)));
        webClient.accept(clientFactory.getContentType().getMediaType()).to(location.toASCIIString(), false);

        return webClient.get(resultClass);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractPolicyTO> T createPolicy(final T policy) {
        Response response = client.getService(PolicyService.class).create(policy);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) getObject(response.getLocation(), PolicyService.class, policy.getClass());
    }

    public static void main(final String[] args) {
        // 1. configure the custom account rule and create an account policy using it
        CustomAccountRuleConf customAccountRuleConf = new CustomAccountRuleConf();
        customAccountRuleConf.setRequiredSubString(ACCOUNT_REQUIRED_SUBSTRING);

        AccountPolicyTO accountPolicy = new AccountPolicyTO();
        accountPolicy.setDescription("Account Policy with custom rules");
        accountPolicy.getRuleConfs().add(customAccountRuleConf);
        accountPolicy = createPolicy(accountPolicy);

        // 2. create a password policy using a custom password rule with no configuration options
        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setDescription("Password Policy with custom rules");
        passwordPolicy.getRuleConfs().add(new CustomPasswordRuleConf());
        passwordPolicy = createPolicy(passwordPolicy);

        // 3. set the account and password policies above for root realm
        List<RealmTO> realms = client.getService(RealmService.class).list();
        RealmTO rootRealm = CollectionUtils.find(realms, new Predicate<RealmTO>() {

            @Override
            public boolean evaluate(final RealmTO realm) {
                return SyncopeConstants.ROOT_REALM.equals(realm.getFullPath());
            }
        });
        rootRealm.setAccountPolicy(accountPolicy.getKey());
        rootRealm.setPasswordPolicy(passwordPolicy.getKey());
        client.getService(RealmService.class).update(rootRealm);

        // 4. obtain sample user: should fail both account and password policy verification
        UserTO userTO = getSampleTO("sample@tirasa.net");

        // 5. attempt to create user -> password policy fails because it has the same value as security answer
        try {
            client.getService(UserService.class).create(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
        }

        // 6. change security answer to comply with password policy
        userTO.setSecurityAnswer("another value");

        // 7. attempt to create user -> account policy fails because username doesn't contain ACCOUNT_REQUIRED_SUBSTRING
        try {
            client.getService(UserService.class).create(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getElements().iterator().next().startsWith("InvalidUsername"));
        }

        // 8. change username to comply with account policy
        userTO.setUsername("pre" + ACCOUNT_REQUIRED_SUBSTRING + "post");

        // 9. finally succeed :-)
        client.getService(UserService.class).create(userTO);
    }
}
