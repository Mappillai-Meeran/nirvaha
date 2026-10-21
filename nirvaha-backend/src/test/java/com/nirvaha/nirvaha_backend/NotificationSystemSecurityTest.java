package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentActionRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.DisputeReviewRequest;
import com.nirvaha.nirvaha_backend.dto.ResidentFeedbackRequest;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.IncidentWorkflowService;
import com.nirvaha.nirvaha_backend.service.NotificationService;
import com.nirvaha.nirvaha_backend.service.ResidentFeedbackService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class NotificationSystemSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository actionRepository;

    @Autowired
    private IncidentFeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IncidentWorkflowService workflowService;

    @Autowired
    private ResidentFeedbackService feedbackService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final String CITIZEN_EMAIL = "notif.citizen@test.puducherry.gov.in";
    private static final String OTHER_CITIZEN_EMAIL = "other.notif.citizen@test.puducherry.gov.in";
    private static final String OFFICER_EMAIL = "notif.officer@test.puducherry.gov.in";

    private User citizenUser;
    private User otherCitizenUser;
    private User officerUser;

    private String citizenJwt;
    private String otherCitizenJwt;
    private String officerJwt;

    private Incident incident;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanupTestData();

        // 1. Create Citizen
        CreateUserRequest req1 = new CreateUserRequest();
        req1.setEmail(CITIZEN_EMAIL);
        req1.setPassword("PondyCitizen#2026");
        req1.setName("Anandhi Citizen");
        req1.setRole(UserRole.CITIZEN);
        userService.createUser(req1);
        citizenUser = userRepository.findByEmail(CITIZEN_EMAIL).orElseThrow();
        citizenJwt = jwtService.generateToken(citizenUser.getId(), citizenUser.getEmail(), citizenUser.getRole().name());

        // 2. Create Other Citizen
        CreateUserRequest req2 = new CreateUserRequest();
        req2.setEmail(OTHER_CITIZEN_EMAIL);
        req2.setPassword("OtherCitizen#2026");
        req2.setName("Bharathi Other");
        req2.setRole(UserRole.CITIZEN);
        userService.createUser(req2);
        otherCitizenUser = userRepository.findByEmail(OTHER_CITIZEN_EMAIL).orElseThrow();
        otherCitizenJwt = jwtService.generateToken(otherCitizenUser.getId(), otherCitizenUser.getEmail(), otherCitizenUser.getRole().name());

        // 3. Create Officer
        CreateUserRequest req3 = new CreateUserRequest();
        req3.setEmail(OFFICER_EMAIL);
        req3.setPassword("Officer#2026Safe");
        req3.setName("Officer Vijay PWD");
        req3.setRole(UserRole.OFFICER);
        userService.createUser(req3);
        officerUser = userRepository.findByEmail(OFFICER_EMAIL).orElseThrow();
        officerJwt = jwtService.generateToken(officerUser.getId(), officerUser.getEmail(), officerUser.getRole().name());

        // 4. Create Incident reported by citizenUser
        Incident inc = new Incident();
        inc.setTitle("Water Main Leakage on Goubert Avenue");
        inc.setDescription("Potable water pipeline rupture near Promenade Beach.");
        inc.setCategory(IncidentCategory.WATER_SUPPLY);
        inc.setStatus(IncidentStatus.REPORTED);
        inc.setSeverity(IncidentSeverity.HIGH);
        inc.setLatitude(new BigDecimal("11.9330000"));
        inc.setLongitude(new BigDecimal("79.8350000"));
        inc.setAddress("Goubert Avenue, White Town, Puducherry");
        inc.setReporter(citizenUser);
        inc.setClassificationTag(DataClassification.OFFICIAL);
        incident = incidentRepository.save(inc);
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        notificationRepository.deleteAll();

        if (incident != null && incident.getId() != null) {
            feedbackRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId())
                    .forEach(feedbackRepository::delete);
            actionRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId())
                    .forEach(actionRepository::delete);
            resolutionRepository.findByIncidentId(incident.getId())
                    .forEach(resolutionRepository::delete);
            evidenceRepository.findByIncidentId(incident.getId())
                    .forEach(evidenceRepository::delete);
            incidentRepository.deleteById(incident.getId());
            incident = null;
        }

        userRepository.findByEmail(CITIZEN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(OTHER_CITIZEN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(OFFICER_EMAIL).ifPresent(userRepository::delete);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Unauthenticated request to /api/notifications is rejected with 401/403")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. User retrieves their own notifications, isolated from other users")
    void userRetrievesOnlyTheirOwnNotifications() throws Exception {
        // Create 2 notifications for citizenUser, and 1 for otherCitizenUser
        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Officer Assigned",
                "Officer Vijay assigned to your case",
                incident.getId(),
                "test-key-1"
        ));

        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                "Work Commenced",
                "Operations commenced on your case",
                incident.getId(),
                "test-key-2"
        ));

        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                otherCitizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Private Alert",
                "Other user alert",
                incident.getId(),
                "test-key-3"
        ));

        // Citizen sees only their 2 notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].recipientId").value(citizenUser.getId()))
                .andExpect(jsonPath("$.content[1].recipientId").value(citizenUser.getId()))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Private Alert"))));

        // Other citizen sees only their 1 notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + otherCitizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Private Alert"));
    }

    @Test
    @DisplayName("3. Unread count returns accurate count for authenticated user")
    void unreadCountReturnsAccurateCount() throws Exception {
        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Notification 1",
                "Message 1",
                incident.getId(),
                "unread-test-1"
        ));

        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                "Notification 2",
                "Message 2",
                incident.getId(),
                "unread-test-2"
        ));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    @DisplayName("4. Marking a notification as read updates read state and read timestamp")
    void markAsReadUpdatesStatusAndTimestamp() throws Exception {
        var createdOpt = notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Test Alert",
                "Test Message",
                incident.getId(),
                "read-test-1"
        ));
        Long notifId = createdOpt.orElseThrow().getId();

        mockMvc.perform(patch("/api/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Verify database state
        Notification reloaded = notificationRepository.findById(notifId).orElseThrow();
        assertThat(reloaded.isRead()).isTrue();
        assertThat(reloaded.getReadAt()).isNotNull();

        // Unread count should now be 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("5. A user cannot mark another user's notification as read (cross-user access denied)")
    void cannotMarkAnotherUsersNotificationAsRead() throws Exception {
        var createdOpt = notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Citizen Private Alert",
                "Confidential",
                incident.getId(),
                "auth-cross-user-1"
        ));
        Long notifId = createdOpt.orElseThrow().getId();

        // otherCitizenUser attempts to mark citizenUser's notification as read
        mockMvc.perform(patch("/api/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + otherCitizenJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("cannot modify notifications belonging to another user")));

        // Notification remains unread
        Notification notif = notificationRepository.findById(notifId).orElseThrow();
        assertThat(notif.isRead()).isFalse();
    }

    @Test
    @DisplayName("6. Mark all as read updates all unread notifications for authenticated user")
    void markAllAsReadUpdatesAllForCurrentUser() throws Exception {
        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Notif 1",
                "Msg 1",
                incident.getId(),
                "bulk-1"
        ));
        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                "Notif 2",
                "Msg 2",
                incident.getId(),
                "bulk-2"
        ));
        notificationService.createNotification(new IncidentLifecycleNotificationEvent(
                otherCitizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                "Other Notif",
                "Other Msg",
                incident.getId(),
                "bulk-3"
        ));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(2));

        // Citizen has 0 unread
        assertThat(notificationRepository.countByRecipientIdAndReadFalse(citizenUser.getId())).isZero();

        // Other citizen still has 1 unread
        assertThat(notificationRepository.countByRecipientIdAndReadFalse(otherCitizenUser.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Duplicate event key does not create duplicate notifications (Idempotency)")
    void duplicateEventKeyDoesNotCreateDuplicates() {
        IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.INCIDENT_ASSIGNED,
                "Duplicate Test",
                "Message",
                incident.getId(),
                "dedup-test-key-100"
        );

        var first = notificationService.createNotification(event);
        var second = notificationService.createNotification(event);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get().getId()).isEqualTo(second.get().getId());

        List<Notification> all = notificationRepository.findAll();
        long count = all.stream().filter(n -> "dedup-test-key-100".equals(n.getDeduplicationKey())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("8. End-to-end incident lifecycle: assignment, work, feedback confirm and dispute generate notifications")
    void fullIncidentLifecycleGeneratesExpectedNotifications() throws Exception {
        // --- 1. Assignment ---
        // Officer assigns incident
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/actions")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":" + officerUser.getId() + ",\"actionType\":\"ASSIGNED\",\"remarks\":\"Assigned to field officer\"}"))
                .andExpect(status().isCreated());

        // Notifications generated: 1 for assigned officer, 1 for reporting resident
        List<Notification> officerNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(officerUser.getId());
        List<Notification> residentNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(citizenUser.getId());

        assertThat(officerNotifs).anyMatch(n -> n.getEventType() == NotificationEventType.INCIDENT_ASSIGNED);
        assertThat(residentNotifs).anyMatch(n -> n.getEventType() == NotificationEventType.INCIDENT_ASSIGNED);

        // --- 2. Start Work ---
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/actions")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":" + officerUser.getId() + ",\"actionType\":\"STARTED\",\"remarks\":\"Field work initiated\"}"))
                .andExpect(status().isCreated());

        residentNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(citizenUser.getId());
        assertThat(residentNotifs).anyMatch(n -> n.getEventType() == NotificationEventType.STATUS_CHANGED && n.getMessage().contains("commenced"));

        // --- 3. Resolve incident and submit feedback ---
        // Setup resolution
        Evidence ev = new Evidence();
        ev.setIncident(incident);
        ev.setEvidenceType(EvidenceType.PHOTO);
        ev.setFileName("leak_fixed.jpg");
        ev.setFileUrl("https://storage.nirvaha.puducherry.gov.in/leak_fixed.jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(250000L);
        ev.setContentHash("1111222233334444555566667777888899990000aaaabbbbccccddddeeee0001");
        ev.setLatitude(new BigDecimal("11.9330000"));
        ev.setLongitude(new BigDecimal("79.8350000"));
        ev.setDescription("Pipeline welded and pressure tested");
        Evidence savedEv = evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(incident);
        res.setOfficer(officerUser);
        res.setResolutionEvidence(savedEv);
        res.setResolutionSimilarityScore(0.95);
        res.setVerificationStatus(VerificationStatus.VERIFIED);
        res.setRemarks("Pipeline restored to full operating pressure");
        Resolution savedRes = resolutionRepository.save(res);

        incident.setStatus(IncidentStatus.RESOLVED);
        incidentRepository.save(incident);

        // Resident disputes resolution
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + citizenJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Water still trickles onto footpath.\"}"))
                .andExpect(status().isCreated());

        // Officer receives RESOLUTION_DISPUTED notification
        officerNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(officerUser.getId());
        assertThat(officerNotifs).anyMatch(n -> n.getEventType() == NotificationEventType.RESOLUTION_DISPUTED && n.getMessage().contains("trickles onto footpath"));

        // --- 4. Officer reviews dispute (ACCEPTED_FOR_REWORK) ---
        IncidentFeedback disputeFb = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(incident.getId()).orElseThrow();

        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/feedback/" + disputeFb.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Second plumbing crew dispatched.\"}"))
                .andExpect(status().isOk());

        // Resident receives DISPUTE_REVIEWED notification
        residentNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(citizenUser.getId());
        assertThat(residentNotifs).anyMatch(n -> n.getEventType() == NotificationEventType.DISPUTE_REVIEWED && n.getTitle().contains("Accepted for Rework"));
    }

    @Test
    @DisplayName("9. Transaction rollback does not create orphaned notifications")
    void transactionRollbackDoesNotCreateNotification() {
        int initialCount = notificationRepository.findAll().size();

        try {
            transactionTemplate.execute(status -> {
                // Publish event inside transaction
                workflowService.assignOfficer(incident.getId(), officerUser.getId());

                // Trigger intentional runtime exception to rollback transaction
                throw new RuntimeException("Simulated failure inside transaction");
            });
        } catch (Exception ignored) {
            // Transaction rolled back
        }

        // Notification repository should NOT have new notifications because transaction rolled back
        int finalCount = notificationRepository.findAll().size();
        assertThat(finalCount).isEqualTo(initialCount);

        // Incident status should remain REPORTED (rollback verified)
        Incident reloaded = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.REPORTED);
    }

    @Test
    @DisplayName("10. Reopened incident rework generates legitimate new notifications without stale suppression")
    void reopenedIncidentReworkGeneratesLegitimateNotifications() throws Exception {
        // First work start -> notification 1
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/actions")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":" + officerUser.getId() + ",\"actionType\":\"STARTED\",\"remarks\":\"Initial field operations\"}"))
                .andExpect(status().isCreated());

        List<Notification> initialNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(citizenUser.getId());
        long initialWorkStarts = initialNotifs.stream()
                .filter(n -> n.getEventType() == NotificationEventType.STATUS_CHANGED && n.getMessage().contains("commenced"))
                .count();
        assertThat(initialWorkStarts).isEqualTo(1);

        // Submit first resolution
        Evidence ev1 = new Evidence();
        ev1.setIncident(incident);
        ev1.setEvidenceType(EvidenceType.PHOTO);
        ev1.setFileName("fix1.jpg");
        ev1.setFileUrl("https://storage.nirvaha.puducherry.gov.in/fix1.jpg");
        ev1.setMimeType("image/jpeg");
        ev1.setFileSize(200000L);
        ev1.setContentHash("1111222233334444555566667777888899990000aaaabbbbccccddddeeee0010");
        ev1.setLatitude(new BigDecimal("11.9330000"));
        ev1.setLongitude(new BigDecimal("79.8350000"));
        ev1.setDescription("Initial fix");
        Evidence savedEv1 = evidenceRepository.save(ev1);

        Resolution res1 = new Resolution();
        res1.setIncident(incident);
        res1.setOfficer(officerUser);
        res1.setResolutionEvidence(savedEv1);
        res1.setResolutionSimilarityScore(0.85);
        res1.setVerificationStatus(VerificationStatus.VERIFIED);
        res1.setRemarks("First repair completed");
        resolutionRepository.save(res1);

        incident.setStatus(IncidentStatus.RESOLVED);
        incidentRepository.save(incident);

        // Resident disputes
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + citizenJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Road surface still leaking.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback disputeFb = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(incident.getId()).orElseThrow();

        // Officer accepts for rework
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/feedback/" + disputeFb.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Rework required.\"}"))
                .andExpect(status().isOk());

        // Officer restarts work for rework -> MUST generate a second distinct notification
        mockMvc.perform(post("/api/incidents/" + incident.getId() + "/actions")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":" + officerUser.getId() + ",\"actionType\":\"STARTED\",\"remarks\":\"Rework field operations commenced\"}"))
                .andExpect(status().isCreated());

        List<Notification> afterReworkNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(citizenUser.getId());
        long afterReworkWorkStarts = afterReworkNotifs.stream()
                .filter(n -> n.getEventType() == NotificationEventType.STATUS_CHANGED && n.getMessage().contains("commenced"))
                .count();
        // Stale suppression must NOT have prevented the second notification!
        assertThat(afterReworkWorkStarts).isEqualTo(2);
    }

    @Test
    @DisplayName("11. Concurrent duplicate notification inserts are handled predictably by database unique constraint")
    void concurrentDuplicateNotificationInsertsHandledPredictably() {
        String dedupKey = "concurrent-race-key-" + System.currentTimeMillis();
        int threadCount = 8;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.List<java.util.concurrent.CompletableFuture<java.util.Optional<com.nirvaha.nirvaha_backend.dto.NotificationResponse>>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                        citizenUser.getId(),
                        NotificationEventType.STATUS_CHANGED,
                        "Concurrent Test",
                        "Testing race condition resilience",
                        incident.getId(),
                        dedupKey
                );
                return notificationService.createNotification(event);
            }, executor));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        executor.shutdown();

        // Every thread should receive an Optional containing the notification response
        Long expectedId = futures.get(0).join().orElseThrow().getId();
        for (var future : futures) {
            var opt = future.join();
            assertThat(opt).isPresent();
            assertThat(opt.get().getId()).isEqualTo(expectedId);
            assertThat(opt.get().getRecipientId()).isEqualTo(citizenUser.getId());
            assertThat(opt.get().getTitle()).isEqualTo("Concurrent Test");
        }

        // Database repository should have exactly 1 record for this deduplication key with matching attributes
        List<Notification> matching = notificationRepository.findAll().stream()
                .filter(n -> dedupKey.equals(n.getDeduplicationKey()))
                .toList();
        assertThat(matching).hasSize(1);
        Notification persisted = matching.get(0);
        assertThat(persisted.getId()).isEqualTo(expectedId);
        assertThat(persisted.getRecipient().getId()).isEqualTo(citizenUser.getId());
        assertThat(persisted.getTitle()).isEqualTo("Concurrent Test");
    }

    @Test
    @DisplayName("12. Unrelated data integrity violation is rethrown and not masked as duplicate")
    void unrelatedDataIntegrityViolationIsNotMaskedAsDuplicate() {
        String dedupKey = "integrity-fail-key-" + System.currentTimeMillis();
        IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                null, // Null title violates not-null database constraint!
                "Testing not-null constraint failure",
                incident.getId(),
                dedupKey
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> notificationService.createNotification(event)
        );

        // Deduplication key must NOT exist in repository
        assertThat(notificationRepository.findByDeduplicationKey(dedupKey)).isEmpty();
    }

    @Test
    @DisplayName("13. Non-transactional event publication does not execute after-commit listener without active transaction")
    void nonTransactionalEventPublicationDoesNotExecuteListener() {
        String dedupKey = "no-tx-event-" + System.currentTimeMillis();
        IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                citizenUser.getId(),
                NotificationEventType.STATUS_CHANGED,
                "No Transaction",
                "Testing fallbackExecution=false",
                incident.getId(),
                dedupKey
        );

        // When published outside any @Transactional boundary:
        eventPublisher.publishEvent(event);

        // Because fallbackExecution is false (strict after-commit guarantee), the listener must NOT fire
        assertThat(notificationRepository.findByDeduplicationKey(dedupKey)).isEmpty();
    }
}
