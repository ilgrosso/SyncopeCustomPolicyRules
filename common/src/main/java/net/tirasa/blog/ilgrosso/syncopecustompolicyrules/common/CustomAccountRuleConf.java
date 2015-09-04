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
package net.tirasa.blog.ilgrosso.syncopecustompolicyrules.common;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.policy.AbstractAccountRuleConf;

@XmlRootElement(name = "customAccountRuleConf")
@XmlType
public class CustomAccountRuleConf extends AbstractAccountRuleConf {

    private static final long serialVersionUID = 2622765748153698352L;

    private String requiredSubString;

    public String getRequiredSubString() {
        return requiredSubString;
    }

    public void setRequiredSubString(final String requiredSubString) {
        this.requiredSubString = requiredSubString;
    }

}
