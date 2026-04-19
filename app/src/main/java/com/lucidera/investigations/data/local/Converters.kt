package com.lucidera.investigations.data.local

import androidx.room.TypeConverter
import com.lucidera.investigations.data.CaseStatus
import com.lucidera.investigations.data.ConfidenceLevel
import com.lucidera.investigations.data.EntityType
import com.lucidera.investigations.data.LeadStatus

class Converters {

    @TypeConverter
    fun fromCaseStatus(value: CaseStatus): String = value.name

    @TypeConverter
    fun toCaseStatus(value: String): CaseStatus = CaseStatus.valueOf(value)

    @TypeConverter
    fun fromLeadStatus(value: LeadStatus): String = value.name

    @TypeConverter
    fun toLeadStatus(value: String): LeadStatus = LeadStatus.valueOf(value)

    @TypeConverter
    fun fromEntityType(value: EntityType): String = value.name

    @TypeConverter
    fun toEntityType(value: String): EntityType = EntityType.valueOf(value)

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String = value.name

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel = ConfidenceLevel.valueOf(value)
}
