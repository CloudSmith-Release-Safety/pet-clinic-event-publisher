import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.UUID;

/**
 * Class responsible for publishing PetClinicReport objects to Amazon SQS
 */
public class PetClinicReportPublisher {

    private final AmazonSQS sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with default AWS region
     *
     * @param queueUrl The URL of the SQS queue to publish to
     */
    public PetClinicReportPublisher(String queueUrl) {
        this(queueUrl, null);
    }

    /**
     * Constructor with specified AWS region
     *
     * @param queueUrl The URL of the SQS queue to publish to
     * @param region   The AWS region (e.g., "us-east-1")
     */
    public PetClinicReportPublisher(String queueUrl, String region) {
        this.queueUrl = queueUrl;
        
        // Initialize SQS client
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder.standard();
        if (region != null && !region.isEmpty()) {
            clientBuilder.withRegion(region);
        }
        this.sqsClient = clientBuilder.build();
        
        // Initialize JSON mapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Publish a PetClinicReport to the SQS queue
     *
     * @param report The report to publish
     * @return The message ID from SQS
     * @throws Exception If there's an error serializing or publishing the message
     */
    public String publishReport(PetClinicReport report) throws Exception {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }

        // Convert report to JSON
        String reportJson = objectMapper.writeValueAsString(report);
        
        // Create a unique deduplication ID for FIFO queues (if needed)
        String messageDeduplicationId = UUID.randomUUID().toString();
        
        // Create a message group ID for FIFO queues (if needed)
        String messageGroupId = "pet-clinic-reports";
        
        // Create the send message request
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(reportJson);
        
        // Add FIFO queue attributes if the queue URL ends with .fifo
        if (queueUrl.endsWith(".fifo")) {
            sendMessageRequest.withMessageDeduplicationId(messageDeduplicationId)
                              .withMessageGroupId(messageGroupId);
        }
        
        // Send the message
        SendMessageResult result = sqsClient.sendMessage(sendMessageRequest);
        
        System.out.println("Successfully published report " + report.getReportInfo().getReportId() + 
                           " to SQS with message ID: " + result.getMessageId());
        
        return result.getMessageId();
    }

    /**
     * Publish a PetClinicReport to the SQS queue with a delay
     *
     * @param report       The report to publish
     * @param delaySeconds The delay in seconds (0-900) before the message is available for processing
     * @return The message ID from SQS
     * @throws Exception If there's an error serializing or publishing the message
     */
    public String publishReportWithDelay(PetClinicReport report, Integer delaySeconds) throws Exception {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }
        
        if (delaySeconds == null || delaySeconds < 0 || delaySeconds > 900) {
            throw new IllegalArgumentException("Delay must be between 0 and 900 seconds");
        }

        // Convert report to JSON
        String reportJson = objectMapper.writeValueAsString(report);
        
        // Create the send message request with delay
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(reportJson)
                .withDelaySeconds(delaySeconds);
        
        // Add FIFO queue attributes if the queue URL ends with .fifo
        if (queueUrl.endsWith(".fifo")) {
            sendMessageRequest.withMessageDeduplicationId(UUID.randomUUID().toString())
                              .withMessageGroupId("pet-clinic-reports");
        }
        
        // Send the message
        SendMessageResult result = sqsClient.sendMessage(sendMessageRequest);
        
        System.out.println("Successfully published report " + report.getReportInfo().getReportId() + 
                           " to SQS with message ID: " + result.getMessageId() + 
                           " (delayed by " + delaySeconds + " seconds)");
        
        return result.getMessageId();
    }

    /**
     * Close resources used by the publisher
     */
    public void shutdown() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
    }

    /**
     * Example usage of the PetClinicReportPublisher
     */
    public static void main(String[] args) {
        // Replace with your actual SQS queue URL
        String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/pet-clinic-reports";
        
        PetClinicReportPublisher publisher = null;
        
        try {
            // Create the publisher
            publisher = new PetClinicReportPublisher(queueUrl, "us-east-1");
            
            // Create a sample report
            PetClinicReport.ReportPeriod period = new PetClinicReport.ReportPeriod(
                "2025-03-01T00:00:00Z",
                "2025-03-31T23:59:59Z"
            );
            
            PetClinicReport.ReportInfo info = new PetClinicReport.ReportInfo(
                "PCR-2025-04-22-12345",
                "Pawsome Pet Care Clinic",
                "2025-04-22T14:30:00Z",
                period,
                "Dr. John Smith",
                "MONTHLY_SUMMARY"
            );
            
            PetClinicReport.ClinicSummary summary = new PetClinicReport.ClinicSummary(
                342, 28, 314, 17, 8, 12, 12.5, 28.3, 4.8
            );
            
            PetClinicReport report = new PetClinicReport(info, summary);
            
            // Publish the report
            String messageId = publisher.publishReport(report);
            System.out.println("Published report with message ID: " + messageId);
            
        } catch (Exception e) {
            System.err.println("Error publishing report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (publisher != null) {
                publisher.shutdown();
            }
        }
    }
}
