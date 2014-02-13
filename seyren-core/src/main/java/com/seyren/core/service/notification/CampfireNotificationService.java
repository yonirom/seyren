/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seyren.core.service.notification;

import static java.lang.String.*;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.seyren.core.domain.Alert;
import com.seyren.core.domain.AlertType;
import com.seyren.core.domain.Check;
import com.seyren.core.domain.Subscription;
import com.seyren.core.domain.SubscriptionType;
import com.seyren.core.exception.NotificationFailedException;
import com.seyren.core.util.config.SeyrenConfig;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madhackerdesigns.jinder.Campfire;
import com.madhackerdesigns.jinder.Room;

@Named
public class CampfireNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CampfireNotificationService.class);
    private final SeyrenConfig seyrenConfig;

    @Inject
    public CampfireNotificationService(SeyrenConfig seyrenConfig) {
        this.seyrenConfig = seyrenConfig;
    }

    @Override
    public void sendNotification(Check check, Subscription subscription, List<Alert> alerts) {
        Room room;
        Campfire campfire;
        String subdomain = StringUtils.trimToNull(seyrenConfig.getCampfireSubdomain());
        String apiToken  = StringUtils.trimToNull(seyrenConfig.getCampfireApiToken());
        String roomName  = StringUtils.trimToNull(seyrenConfig.getCampfireRoom());

        if (subdomain == null || apiToken == null)
        {
            LOGGER.warn("Campfire requires CAMPFIRE_SUBDOMAIN and CAMPFIRE_APITOKEN to be set before sending notifications to Campfire");
            return;
        }

        try {
            campfire = new Campfire(subdomain, apiToken);
            if (roomName == null) {
                room = campfire.rooms().get(0);
            } else {
                room = campfire.findRoomByName(seyrenConfig.getCampfireRoom());
            }
            if (room != null) {
              room.join();
              room.speak(createMessage(check));
            }
        } catch (IOException e) {
            throw new NotificationFailedException("Failed to send notification to " + subscription.getTarget() + " from ", e);
        }

    }

    private String createMessage(Check check) {
        String checkUrl = seyrenConfig.getBaseUrl() + "/#/checks/" + check.getId();

        if (check.getState() == AlertType.ERROR) {
            return format("Seyren Alert! Service %s changed to state [CRIT] | %s", check.getName(), checkUrl);
        }
        if (check.getState() == AlertType.WARN) {
            return format("Seyren Alert! Service %s changed to state [WARN] | %s", check.getName(), checkUrl);
        }
        if (check.getState() == AlertType.OK) {
            return format("Seyren Alert! Service %s changed to state [OK] | %s", check.getName(), checkUrl);
        }

        return "";
    }

    @Override
    public boolean canHandle(SubscriptionType subscriptionType) {
        return subscriptionType == SubscriptionType.CAMPFIRE;
    }

}
