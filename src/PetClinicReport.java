import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Plain Old Java Object (POJO) implementation for Pet Clinic Report
 */
public class PetClinicReport {

    private ReportInfo reportInfo;
    private ClinicSummary clinicSummary;

    /**
     * Class representing report information
     */
    public static class ReportInfo {
        private String reportId;
        private String clinicName;
        private String reportDate;
        private ReportPeriod reportPeriod;
        private String generatedBy;
        private String reportType;
    }

    /**
     * Class representing the report period
     */
    public static class ReportPeriod {
        private String startDate;
        private String endDate;
    }

    /**
     * Class representing clinic summary statistics
     */
    public static class ClinicSummary {
        private int totalAppointments;
        private int newPatients;
        private int returningPatients;
        private int canceledAppointments;
        private int noShows;
        private int emergencyCases;
        private double averageWaitTime;
        private double averageVisitDuration;
        private double patientSatisfactionScore;
    }
}
