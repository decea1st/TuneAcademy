package com.tuneacademy.backend.tutoring;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringRequestRepository extends JpaRepository<TutoringRequest, Long> {

    // "This request, but only if it was sent to this instructor." Anyone else finds nothing (404),
    // the same answer as a request that doesn't exist, so request ids can't be probed.
    Optional<TutoringRequest> findByIdAndInstructorId(Long id, Long instructorId);

    boolean existsByLearnerIdAndInstructorIdAndStatus(Long learnerId, Long instructorId, RequestStatus status);

    List<TutoringRequest> findByInstructorIdAndStatusInOrderByCreatedAtDesc(Long instructorId,
                                                                           Collection<RequestStatus> statuses);

    List<TutoringRequest> findByLearnerIdOrderByCreatedAtDesc(Long learnerId);
}
