package com.carland.carland_service.service;

import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.entity.PushCampaign;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.repository.PushCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Kuyruğa alınan admin push kampanyasını arka planda FCM sendEachForMulticast (500) ile işler.
 * en: Processes a queued admin push campaign in the background via FCM sendEachForMulticast (500).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushCampaignWorker {

    static final int FCM_MULTICAST_LIMIT = 500;

    private final PushCampaignRepository pushCampaignRepository;
    private final AdminPushCampaignService adminPushCampaignService;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;

    @Async("pushCampaignExecutor")
    public void run(Long campaignId) {
        try {
            process(campaignId);
        } catch (Exception e) {
            log.error("Push campaign failed id={}", campaignId, e);
            markFailed(campaignId, e.getMessage());
        }
    }

    private void process(Long campaignId) {
        PushCampaign campaign = pushCampaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.error("Push campaign missing id={}", campaignId);
            return;
        }
        campaign.setStatus(PushCampaign.SENDING);
        campaign.setStartedAt(LocalDateTime.now());
        pushCampaignRepository.save(campaign);

        List<DeviceToken> audience = adminPushCampaignService.loadAudience(campaign).stream()
                .filter(dt -> dt.getDeviceToken() != null && !dt.getDeviceToken().isBlank())
                .toList();
        campaign.setAudienceCount(audience.size());
        pushCampaignRepository.save(campaign);

        if (audience.isEmpty()) {
            campaign.setStatus(PushCampaign.COMPLETED);
            campaign.setFinishedAt(LocalDateTime.now());
            pushCampaignRepository.save(campaign);
            return;
        }

        String title = campaign.getTitle();
        String body = campaign.getBody();
        int success = 0;
        int failed = 0;

        for (int from = 0; from < audience.size(); from += FCM_MULTICAST_LIMIT) {
            int to = Math.min(from + FCM_MULTICAST_LIMIT, audience.size());
            List<DeviceToken> slice = audience.subList(from, to);
            List<String> tokens = slice.stream().map(DeviceToken::getDeviceToken).toList();
            List<Boolean> results;
            try {
                results = pushNotificationService.sendEachForMulticast(title, body, tokens);
            } catch (Exception e) {
                log.error("Multicast batch failed campaignId={} from={} to={}", campaignId, from, to, e);
                failed += slice.size();
                campaign.setFailedCount(failed);
                pushCampaignRepository.save(campaign);
                continue;
            }
            List<Notification> inbox = new ArrayList<>();
            for (int i = 0; i < slice.size(); i++) {
                boolean ok = i < results.size() && Boolean.TRUE.equals(results.get(i));
                if (ok) {
                    success++;
                    inbox.add(Notification.builder()
                            .created(LocalDate.now())
                            .customerId(slice.get(i).getUserId())
                            .notificationText(body)
                            .title(title)
                            .status("ACTIVE")
                            .isRead(false)
                            .type("BULK")
                            .build());
                } else {
                    failed++;
                }
            }
            if (!inbox.isEmpty()) {
                notificationRepository.saveAll(inbox);
            }
            campaign.setSuccessCount(success);
            campaign.setFailedCount(failed);
            pushCampaignRepository.save(campaign);
        }

        campaign.setStatus(PushCampaign.COMPLETED);
        campaign.setFinishedAt(LocalDateTime.now());
        pushCampaignRepository.save(campaign);
        log.info("Push campaign done id={} success={} failed={}", campaignId, success, failed);
    }

    private void markFailed(Long campaignId, String message) {
        pushCampaignRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setStatus(PushCampaign.FAILED);
            campaign.setFinishedAt(LocalDateTime.now());
            if (message != null && !message.isBlank()) {
                campaign.setErrorMessage(message.length() > 512 ? message.substring(0, 512) : message);
            }
            pushCampaignRepository.save(campaign);
        });
    }
}
