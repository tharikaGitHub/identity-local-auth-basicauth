/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.basicauth.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.basicauth.BasicAuthenticator;
import org.wso2.carbon.identity.captcha.util.CaptchaConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.multi.attribute.login.mgt.MultiAttributeLoginService;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component(
        name = "identity.application.authenticator.basicauth.component",
        immediate = true)
public class BasicAuthenticatorServiceComponent {

    private static final Log log = LogFactory.getLog(BasicAuthenticatorServiceComponent.class);

    private static RealmService realmService;

    public static RealmService getRealmService() {

        return realmService;
    }

    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        log.debug("Setting the Realm Service");
        BasicAuthenticatorServiceComponent.realmService = realmService;
    }

    @Activate
    protected void activate(ComponentContext ctxt) {

        buildReCaptchaFilterProperties();
        try {
            BasicAuthenticator basicAuth = new BasicAuthenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), basicAuth, null);
            if (log.isDebugEnabled()) {
                log.info("BasicAuthenticator bundle is activated");
            }
        } catch (Throwable e) {
            log.error("SAMLSSO Authenticator bundle activation Failed", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {

        if (log.isDebugEnabled()) {
            log.info("BasicAuthenticator bundle is deactivated");
        }
    }

    protected void unsetRealmService(RealmService realmService) {

        log.debug("UnSetting the Realm Service");
        BasicAuthenticatorServiceComponent.realmService = null;
    }

    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService identityGovernanceService) {

        BasicAuthenticatorDataHolder.getInstance().setIdentityGovernanceService(identityGovernanceService);
    }

    protected void unsetIdentityGovernanceService(IdentityGovernanceService identityGovernanceService) {

        BasicAuthenticatorDataHolder.getInstance().setIdentityGovernanceService(null);
    }

    @Reference(
            name = "MultiAttributeLoginService",
            service = MultiAttributeLoginService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetMultiAttributeLoginService")
    protected void setMultiAttributeLoginService(MultiAttributeLoginService multiAttributeLogin) {

        BasicAuthenticatorDataHolder.getInstance().setMultiAttributeLogin(multiAttributeLogin);
    }

    protected void unsetMultiAttributeLoginService(MultiAttributeLoginService multiAttributeLogin) {

        BasicAuthenticatorDataHolder.getInstance().setMultiAttributeLogin(null);
    }

    /**
     * Read the captcha-config.properties file located in repository/conf/identity directory and set the
     * configurations required to enable recaptcha in the Data holder.
     */
    private void buildReCaptchaFilterProperties() {

        Path path = Paths.get(IdentityUtil.getIdentityConfigDirPath(), CaptchaConstants.CAPTCHA_CONFIG_FILE_NAME);

        if (Files.exists(path)) {
            Properties properties = new Properties();
            try (Reader in = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                properties.load(in);
            } catch (IOException e) {
                log.error("Error while loading '" + CaptchaConstants.CAPTCHA_CONFIG_FILE_NAME + "' configuration " +
                        "file", e);
            }

            boolean reCaptchaEnabled = Boolean.valueOf(properties.getProperty(CaptchaConstants
                    .RE_CAPTCHA_ENABLED));

            if (reCaptchaEnabled) {
                BasicAuthenticatorDataHolder.getInstance().setRecaptchaConfigs(properties);
            }
        }
    }
}
