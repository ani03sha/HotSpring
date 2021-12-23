package org.redquark.hotspring.document;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redquark.hotspring.document.services.NotificationService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3EventHandler implements RequestHandler<S3Event, String> {

    private final NotificationService notificationService;

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        log.info("Lambda function is invoked: Processing the uploads");
        String event = s3Event.getRecords().get(0).getEventName();
        String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String key = s3Event.getRecords().get(0).getS3().getObject().getKey();
        log.info("Received {} event for key: {} in bucket: {}", event, key, bucket);
        return notificationService.sendFileReceivedNotification(bucket, key);
    }
}
