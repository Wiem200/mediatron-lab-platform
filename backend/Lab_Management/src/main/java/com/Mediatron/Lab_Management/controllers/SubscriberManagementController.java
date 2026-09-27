package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.APIResponse;
import com.Mediatron.Lab_Management.data_dto.SubscriberManagementDTO;
import com.Mediatron.Lab_Management.data_dto.SubscriberStatusUpdateRequest;
import com.Mediatron.Lab_Management.services.SubscriberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subscribers")
@CrossOrigin(origins = "*")
@PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
public class SubscriberManagementController {

    private final SubscriberService subscriberService;

    public SubscriberManagementController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubscriberManagementDTO>> getAllSubscribers() {
        return ResponseEntity.ok(subscriberService.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriberManagementDTO>> searchSubscribers(
            @RequestParam(name = "email", defaultValue = "") String email
    ) {
        return ResponseEntity.ok(subscriberService.searchByEmail(email));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<SubscriberManagementDTO>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody SubscriberStatusUpdateRequest request
    ) {
        SubscriberManagementDTO updated = subscriberService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(APIResponse.ok("Subscriber status updated.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteSubscriber(@PathVariable Integer id) {
        subscriberService.delete(id);
        return ResponseEntity.ok(APIResponse.ok("Subscriber removed successfully.", null));
    }
}
