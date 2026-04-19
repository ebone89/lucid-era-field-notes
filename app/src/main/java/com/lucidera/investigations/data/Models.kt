package com.lucidera.investigations.data

enum class CaseStatus {
    ACTIVE,
    HOLD,
    VERIFIED,
    ARCHIVED
}

enum class LeadStatus {
    OPEN,
    ARCHIVED,
    VERIFIED,
    DEFERRED
}

enum class EntityType {
    PERSON,
    COMPANY,
    DOMAIN,
    ORGANIZATION,
    ADDRESS,
    ASSET
}

enum class ConfidenceLevel {
    UNCONFIRMED,
    PROBABLE,
    VERIFIED
}
