package com.airtribe.draftly.domain;

/**
 * Lifecycle of a reply draft.
 *
 * <pre>
 *   SUGGESTED ──approve──► APPROVED ──send──► SENT
 *       │                     ▲                  │
 *       ├──edit────► EDITED ──┘                  │ (send fails)
 *       │                                        ▼
 *       └──reject──► REJECTED                  FAILED ──retry──► SENT
 * </pre>
 *
 * Only APPROVED or EDITED drafts are allowed to be sent.
 */
public enum DraftStatus {
    SUGGESTED,
    APPROVED,
    EDITED,
    REJECTED,
    SENT,
    FAILED
}
