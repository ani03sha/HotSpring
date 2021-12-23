package org.redquark.hotspring.document.services;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.redquark.hotspring.document.configs.DownloaderConfig;
import org.redquark.hotspring.document.configs.LambdaConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class NotificationService {

    private final DownloaderConfig downloaderConfig;

    public NotificationService(LambdaConfig lambdaConfig) {
        this.downloaderConfig = lambdaConfig.getDownloaderConfig();
    }

    public String sendFileReceivedNotification(String bucket, String key) {
        String response = "";
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject s3FileJson = new JSONObject();
            s3FileJson.put("bucket", bucket);
            s3FileJson.put("key", key);
            HttpEntity<String> request = new HttpEntity<>(s3FileJson.toString(), httpHeaders);
            response = restTemplate.postForObject(downloaderConfig.getDownloadEndpoint(), request, String.class);
        } catch (JSONException e) {
            log.error("Could not send notification due to: {}", e.getMessage(), e);
        }
        return response;
    }
}
