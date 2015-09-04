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
package net.tirasa.blog.ilgrosso.syncopecustompolicyrules.core;

import net.tirasa.blog.ilgrosso.syncopecustompolicyrules.common.CustomPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.misc.policy.PasswordPolicyException;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRuleConfClass;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

@PasswordRuleConfClass(CustomPasswordRuleConf.class)
public class CustomPasswordRule implements PasswordRule {

    @Transactional(readOnly = true)
    @Override
    public void enforce(final PasswordRuleConf conf, final User user) {
        if (user.getClearPassword().equals(user.getSecurityAnswer())) {
            throw new PasswordPolicyException("Password has same value as security answer");
        }
    }

}
