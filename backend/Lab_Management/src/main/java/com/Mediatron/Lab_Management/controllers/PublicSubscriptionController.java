package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.APIResponse;
import com.Mediatron.Lab_Management.data_dto.SubscriberDTO;
import com.Mediatron.Lab_Management.services.SubscriberService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicSubscriptionController {

    private final SubscriberService subscriberService;

    public PublicSubscriptionController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<APIResponse<Void>> subscribe(@Valid @RequestBody SubscriberDTO dto) {
        boolean created = subscriberService.subscribe(dto);

        if (!created) {
            return ResponseEntity.badRequest()
                    .body(APIResponse.error("Already subscribed."));
        }

        return ResponseEntity.ok(APIResponse.ok("Subscription successful.", null));
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam("token") String token) {
        boolean unsubscribed = subscriberService.unsubscribe(token);

        String title = unsubscribed ? "Unsubscribed successfully" : "Invalid unsubscribe link";
        String message = unsubscribed
                ? "You have been unsubscribed from MEDIATRON email updates."
                : "This unsubscribe link is invalid or expired.";

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: grid;
                            place-items: center;
                            background: #f6f1e8;
                            color: #172b55;
                            font-family: Arial, sans-serif;
                        }
                        .card {
                            max-width: 520px;
                            background: #ffffff;
                            border-radius: 24px;
                            padding: 36px;
                            box-shadow: 0 20px 60px rgba(23, 43, 85, 0.18);
                            border: 1px solid #eadcc2;
                        }
                        .eyebrow {
                            color: #bd9650;
                            font-weight: 800;
                            letter-spacing: .18em;
                            text-transform: uppercase;
                            font-size: 12px;
                        }
                        h1 {
                            margin: 12px 0;
                            font-size: 28px;
                        }
                        p {
                            line-height: 1.7;
                            color: #50607c;
                        }
                        a {
                            color: #172b55;
                            font-weight: 700;
                        }
                    </style>
                </head>
                <body>
                    <main class="card">
                        <div class="eyebrow">MEDIATRON Notifications</div>
                        <h1>%s</h1>
                        <p>%s</p>
                        <p><a href="http://localhost:5173/">Return to MEDIATRON</a></p>
                    </main>
                </body>
                </html>
                """.formatted(title, title, message);

        return ResponseEntity.ok(html);
    }
}