package com.tuneacademy.backend.tutoring;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.profile.InstructorProfileRepository;
import com.tuneacademy.backend.tutoring.TutoringController.IncomingRequest;
import com.tuneacademy.backend.tutoring.TutoringController.MyRequest;
import com.tuneacademy.backend.user.Role;
import com.tuneacademy.backend.user.User;
import com.tuneacademy.backend.user.UserRepository;

@Service
public class TutoringService {

    private static final String ALREADY_PENDING = "You already have a pending request with this instructor.";

    private final TutoringRequestRepository requests;
    private final EngagementRepository engagements;
    private final UserRepository users;
    private final InstructorProfileRepository instructorProfiles;

    public TutoringService(TutoringRequestRepository requests, EngagementRepository engagements,
                           UserRepository users, InstructorProfileRepository instructorProfiles) {
        this.requests = requests;
        this.engagements = engagements;
        this.users = users;
        this.instructorProfiles = instructorProfiles;
    }

    public MyRequest send(User learner, Long instructorId, String message) {
        if (learner.getRole() != Role.LEARNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only learners can request lessons.");
        }
        if (!instructorProfiles.existsById(instructorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found.");
        }
        // Friendly check first, so the usual case gets a clear message...
        if (requests.existsByLearnerIdAndInstructorIdAndStatus(learner.getId(), instructorId, RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ALREADY_PENDING);
        }
        TutoringRequest saved;
        try {
            saved = requests.save(new TutoringRequest(learner.getId(), instructorId, message.trim()));
        } catch (DataIntegrityViolationException e) {
            // ...but the real guarantee is the database's one_pending_request_per_pair index,
            // which also catches two clicks arriving at the same moment.
            throw new ResponseStatusException(HttpStatus.CONFLICT, ALREADY_PENDING);
        }
        return toMyRequest(saved, users.findById(instructorId).orElseThrow());
    }

    /** The instructor's panel: pending requests to answer, then accepted ones. Declined ones are hidden. */
    public List<IncomingRequest> incoming(Long instructorId) {
        List<TutoringRequest> list = requests.findByInstructorIdAndStatusInOrderByCreatedAtDesc(
            instructorId, List.of(RequestStatus.PENDING, RequestStatus.ACCEPTED));
        Map<Long, User> learners = usersById(list.stream().map(TutoringRequest::getLearnerId).toList());
        return list.stream().map(r -> toIncoming(r, learners.get(r.getLearnerId()))).toList();
    }

    /** The learner's own requests, every status, newest first. */
    public List<MyRequest> mine(Long learnerId) {
        List<TutoringRequest> list = requests.findByLearnerIdOrderByCreatedAtDesc(learnerId);
        Map<Long, User> instructors = usersById(list.stream().map(TutoringRequest::getInstructorId).toList());
        return list.stream().map(r -> toMyRequest(r, instructors.get(r.getInstructorId()))).toList();
    }

    /**
     * Accepting changes two tables: the request becomes ACCEPTED, and its engagement is created.
     * @Transactional makes that all-or-nothing: if creating the engagement fails, the ACCEPTED is
     * rolled back too, so there's never an accepted request without an engagement.
     */
    @Transactional
    public IncomingRequest accept(Long instructorId, Long requestId) {
        TutoringRequest request = pendingRequestFor(instructorId, requestId);
        request.answer(RequestStatus.ACCEPTED);
        requests.save(request);
        engagements.save(new Engagement(request.getId()));
        return toIncoming(request, users.findById(request.getLearnerId()).orElseThrow());
    }

    @Transactional
    public IncomingRequest decline(Long instructorId, Long requestId) {
        TutoringRequest request = pendingRequestFor(instructorId, requestId);
        request.answer(RequestStatus.DECLINED);
        requests.save(request);
        return toIncoming(request, users.findById(request.getLearnerId()).orElseThrow());
    }

    private TutoringRequest pendingRequestFor(Long instructorId, Long requestId) {
        TutoringRequest request = requests.findByIdAndInstructorId(requestId, instructorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found."));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request has already been answered.");
        }
        return request;
    }

    // ---- Building what the browser sees ----

    private static IncomingRequest toIncoming(TutoringRequest r, User learner) {
        return new IncomingRequest(r.getId(), learner.getFullName(), learner.getSkillLevel(), learner.getInstruments(),
            r.getMessage(), r.getStatus(), r.getCreatedAt(), contactEmail(r, learner));
    }

    private static MyRequest toMyRequest(TutoringRequest r, User instructor) {
        return new MyRequest(r.getId(), instructor.getId(), instructor.getFullName(), r.getMessage(), r.getStatus(),
            r.getCreatedAt(), contactEmail(r, instructor));
    }

    /** The only place an email goes into a response: the other person's, and only once the request is accepted. */
    private static String contactEmail(TutoringRequest r, User otherPerson) {
        return r.getStatus() == RequestStatus.ACCEPTED ? otherPerson.getEmail() : null;
    }

    // One query for all the people in a list, instead of one query per row.
    private Map<Long, User> usersById(List<Long> ids) {
        return users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
