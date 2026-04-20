package com.lucidera.investigations.data.local

import androidx.room.TypeConverter
import com.lucidera.investigations.data.AttachmentType
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.ConfidenceLevel
import com.lucidera.investigations.data.EntityType
import com.lucidera.investigations.data.LeadStatus

class Converters {

    @TypeConverter
    fun fromCaseStatus(value: CaseStatus): String = value.name

    @TypeConverter
    fun toCaseStatus(value: String): CaseStatus =
        runCatching { CaseStatus.valueOf(value) }.getOrDefault(CaseStatus.ACTIVE)

    @TypeConverter
    fun fromLeadStatus(value: LeadStatus): String = value.name

    @TypeConverter
    fun toLeadStatus(value: String): LeadStatus =
        runCatching { LeadStatus.valueOf(value) }.getOrDefault(LeadStatus.OPEN)

    @TypeConverter
    fun fromEntityType(value: EntityType): String = value.name

    @TypeConverter
    fun toEntityType(value: String): EntityType =
        runCatching { EntityType.valueOf(value) }.getOrDefault(EntityType.ORGANIZATION)

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String = value.name

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel =
        runCatching { ConfidenceLevel.valueOf(value) }.getOrDefault(ConfidenceLevel.UNCONFIRMED)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType =
        runCatching { AttachmentType.valueOf(value) }.getOrDefault(AttachmentType.GALLERY)
}
