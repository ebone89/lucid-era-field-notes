package com.lucidera.investigations.data

import com.lucidera.investigations.data.local.CaseDao
import com.lucidera.investigations.data.local.EntityProfileDao
import com.lucidera.investigations.data.local.LeadDao
import com.lucidera.investigations.data.local.AttachmentDao
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity
import com.lucidera.investigations.data.network.WaybackApi
import com.lucidera.investigations.data.network.WaybackLookupResult
import kotlinx.coroutines.flow.Flow

class InvestigationRepository(
    private val caseDao: CaseDao,
    private val leadDao: LeadDao,
    private val entityDao: EntityProfileDao,
    private val attachmentDao: AttachmentDao,
    private val waybackApi: WaybackApi
) {

    val allCases: Flow<List<InvestigationCaseEntity>> = caseDao.observeAllCases()

    fun searchCases(query: String): Flow<List<InvestigationCaseEntity>> = caseDao.searchCases(query)
    val openLeadCount: Flow<Int> = leadDao.observeLeadCountByStatus(LeadStatus.OPEN)
    val verifiedLeadCount: Flow<Int> = leadDao.observeLeadCountByStatus(LeadStatus.VERIFIED)
    val entityCount: Flow<Int> = entityDao.observeEntityCount()

    suspend fun seedIfEmpty() {
        if (caseDao.countCases() > 0) {
            return
        }

        val caseOneId = caseDao.insertCase(
            InvestigationCaseEntity(
                caseCode = "CASE-0001",
                title = "Palatka Power Map",
                essentialQuestion = "What public relationships tie the Palatka power project back to contractors, donors, and decision makers?",
                primarySubject = "Palatka power network",
                status = CaseStatus.ACTIVE,
                classification = "Internal working file",
                leadInvestigator = "Ethan Bradley",
                summary = "Local accountability case focused on influence, procurement, and ownership relationships around the Palatka power map.",
                caseFolderName = "CASE-0001_Palatka_Power_Map",
                masterNoteName = "CASE-0001_Palatka_Power_Map.md",
                savePath = "A:\\Obsidian_Vaults\\Main-Notes\\03_Organizations\\03_Lucid_Era_Group\\031-Lucid_Era_Investigations\\10_Investigations\\CASE-0001_Palatka_Power_Map",
                publicationThreshold = "Three independent records for every ownership or donor claim."
            )
        )

        leadDao.insertLead(
            LeadEntity(
                caseId = caseOneId,
                sourceName = "Sunbiz",
                sourceUrl = "https://search.sunbiz.org/",
                archiveUrl = "",
                tags = "corporate-records, officers",
                summary = "Check officer overlap between contractors, donors, and related entities.",
                status = LeadStatus.OPEN
            )
        )

        entityDao.insertEntity(
            EntityProfileEntity(
                caseId = caseOneId,
                name = "Palatka Utilities",
                entityType = EntityType.ORGANIZATION,
                confidence = ConfidenceLevel.PROBABLE,
                aliases = "",
                summary = "Initial subject organization for mapping procurement and governance relationships.",
                identifier = "ORG-PALATKA-UTIL"
            )
        )

        val caseTwoId = caseDao.insertCase(
            InvestigationCaseEntity(
                caseCode = "CASE-0002",
                title = "PCSD Records Request",
                essentialQuestion = "What records can be obtained from PCSD that clarify the underlying public-interest question in this case?",
                primarySubject = "Putnam County School District records trail",
                status = CaseStatus.ACTIVE,
                classification = "Active records case",
                leadInvestigator = "Ethan Bradley",
                summary = "Public-records case centered on request tracking, follow-up, and document handling.",
                caseFolderName = "CASE-0002_PCSD_Records_Request",
                masterNoteName = "CASE-0002_PCSD_Records_Request.md",
                savePath = "A:\\Obsidian_Vaults\\Main-Notes\\03_Organizations\\03_Lucid_Era_Group\\031-Lucid_Era_Investigations\\10_Investigations\\CASE-0002_PCSD_Records_Request",
                publicationThreshold = "Primary records plus at least two corroborating sources before any public claim."
            )
        )

        leadDao.insertLead(
            LeadEntity(
                caseId = caseTwoId,
                sourceName = "Request log",
                sourceUrl = "",
                archiveUrl = "",
                tags = "records-request, timeline",
                summary = "Track request dates, agency responses, and appeal deadlines in one place.",
                status = LeadStatus.OPEN
            )
        )

        val caseThreeId = caseDao.insertCase(
            InvestigationCaseEntity(
                caseCode = "CASE-0003",
                title = "Gilbert Road Data Center",
                essentialQuestion = "Who is advancing the Gilbert Road data center project, what public infrastructure makes it viable, and what paper trail ties together the land, utility, and county sides?",
                primarySubject = "135 Gilbert Road / East Palatka data center site",
                status = CaseStatus.ACTIVE,
                classification = "Internal working document",
                leadInvestigator = "Ethan Bradley",
                summary = "Large East Palatka land assemblage being marketed for hyperscale data center use, with PUD, power, water, and tax questions at the center of the reporting.",
                caseFolderName = "CASE-0003_Gilbert_Road_Data_Center",
                masterNoteName = "CASE-0003_Gilbert_Road_Data_Center.md",
                savePath = "A:\\Obsidian_Vaults\\Main-Notes\\03_Organizations\\03_Lucid_Era_Group\\031-Lucid_Era_Investigations\\10_Investigations\\CASE-0003_Gilbert_Road_Data_Center",
                publicationThreshold = "Three independent, non-circular sources for any public finding, with primary documentation for power, utility, tax, or land claims."
            )
        )

        leadDao.insertLead(
            LeadEntity(
                caseId = caseThreeId,
                sourceName = "Parcel table",
                sourceUrl = "",
                archiveUrl = "",
                tags = "parcel, land-records",
                summary = "Use the parcel list as the search key for deeds, tax rolls, easements, agendas, and rezoning notices.",
                status = LeadStatus.OPEN
            )
        )
        leadDao.insertLead(
            LeadEntity(
                caseId = caseThreeId,
                sourceName = "Power corridor claim",
                sourceUrl = "",
                archiveUrl = "",
                tags = "power, infrastructure",
                summary = "Separate corridor adjacency from actual deliverability, price, and upgrade responsibility.",
                status = LeadStatus.OPEN
            )
        )
        entityDao.insertEntity(
            EntityProfileEntity(
                caseId = caseThreeId,
                name = "Rayonier / Raydient",
                entityType = EntityType.COMPANY,
                confidence = ConfidenceLevel.PROBABLE,
                aliases = "Raydient",
                summary = "Seller-side entity posture for the Gilbert Road site. Corporate role is strong; some corporate-history claims still need primary verification.",
                identifier = "ENT-RAYONIER-COMPANY"
            )
        )
    }

    fun observeCase(caseId: Long): Flow<InvestigationCaseEntity?> = caseDao.observeCase(caseId)

    fun observeLeads(caseId: Long): Flow<List<LeadEntity>> = leadDao.observeLeadsForCase(caseId)

    fun observeEntities(caseId: Long): Flow<List<EntityProfileEntity>> = entityDao.observeEntitiesForCase(caseId)

    fun observeAttachments(caseId: Long): Flow<List<CaseAttachmentEntity>> = attachmentDao.observeAttachmentsForCase(caseId)

    suspend fun addCase(draft: CaseDraft) {
        caseDao.insertCase(
            InvestigationCaseEntity(
                caseCode = draft.caseCode,
                title = draft.title,
                essentialQuestion = draft.essentialQuestion,
                primarySubject = draft.primarySubject,
                status = draft.status,
                classification = draft.classification,
                leadInvestigator = draft.leadInvestigator,
                summary = draft.summary,
                caseFolderName = draft.caseFolderName,
                masterNoteName = draft.masterNoteName,
                savePath = draft.savePath,
                publicationThreshold = draft.publicationThreshold
            )
        )
    }

    suspend fun addLead(caseId: Long, draft: LeadDraft) {
        leadDao.insertLead(
            LeadEntity(
                caseId = caseId,
                sourceName = draft.sourceName,
                sourceUrl = draft.sourceUrl,
                archiveUrl = draft.archiveUrl,
                tags = draft.tags,
                summary = draft.summary,
                status = draft.status,
                latitude = draft.latitude,
                longitude = draft.longitude
            )
        )
    }

    suspend fun updateLead(lead: LeadEntity, draft: LeadDraft) {
        leadDao.insertLead(
            lead.copy(
                sourceName = draft.sourceName,
                sourceUrl = draft.sourceUrl,
                archiveUrl = draft.archiveUrl,
                tags = draft.tags,
                summary = draft.summary,
                status = draft.status,
                latitude = draft.latitude,
                longitude = draft.longitude
            )
        )
    }

    suspend fun deleteLead(leadId: Long) {
        leadDao.deleteLead(leadId)
    }

    suspend fun deleteCase(caseId: Long) {
        caseDao.deleteCase(caseId)
    }

    suspend fun updateLeadStatus(leadId: Long, status: LeadStatus) {
        leadDao.updateLeadStatus(leadId, status)
    }

    suspend fun addEntity(caseId: Long, draft: EntityDraft) {
        entityDao.insertEntity(
            EntityProfileEntity(
                caseId = caseId,
                name = draft.name,
                entityType = draft.entityType,
                confidence = draft.confidence,
                aliases = draft.aliases,
                summary = draft.summary,
                identifier = draft.identifier
            )
        )
    }

    suspend fun updateEntity(entity: EntityProfileEntity, draft: EntityDraft) {
        entityDao.insertEntity(
            entity.copy(
                name = draft.name,
                entityType = draft.entityType,
                confidence = draft.confidence,
                aliases = draft.aliases,
                summary = draft.summary,
                identifier = draft.identifier
            )
        )
    }

    suspend fun deleteEntity(entityId: Long) {
        entityDao.deleteEntity(entityId)
    }

    suspend fun addAttachment(caseId: Long, draft: AttachmentDraft) {
        attachmentDao.insertAttachment(
            CaseAttachmentEntity(
                caseId = caseId,
                uri = draft.uri,
                fileName = draft.fileName,
                mimeType = draft.mimeType,
                caption = draft.caption,
                attachmentType = draft.attachmentType,
                gpsLat = draft.gpsLat,
                gpsLon = draft.gpsLon,
                capturedAt = draft.capturedAt,
                deviceModel = draft.deviceModel,
                fileHash = draft.fileHash
            )
        )
    }

    suspend fun updateAttachmentCaption(attachmentId: Long, caption: String) {
        attachmentDao.updateAttachmentCaption(attachmentId, caption)
    }

    suspend fun updateTranscription(attachmentId: Long, transcription: String) {
        attachmentDao.updateTranscription(attachmentId, transcription)
    }

    suspend fun deleteAttachment(attachmentId: Long) {
        attachmentDao.deleteAttachment(attachmentId)
    }

    suspend fun lookupArchive(url: String): WaybackLookupResult {
        val response = waybackApi.lookupAvailability(url)
        val closest = response.archivedSnapshots.closest
            ?: throw IllegalStateException("No archived snapshot found for this URL.")

        return WaybackLookupResult(
            originalUrl = url,
            archiveUrl = closest.url,
            timestamp = closest.timestamp,
            available = closest.available,
            status = closest.status
        )
    }
}

data class CaseDraft(
    val caseCode: String,
    val title: String,
    val essentialQuestion: String,
    val primarySubject: String,
    val classification: String,
    val leadInvestigator: String,
    val summary: String,
    val caseFolderName: String,
    val masterNoteName: String,
    val savePath: String,
    val publicationThreshold: String,
    val status: CaseStatus = CaseStatus.ACTIVE
)

data class LeadDraft(
    val sourceName: String,
    val sourceUrl: String,
    val archiveUrl: String,
    val tags: String = "",
    val summary: String,
    val status: LeadStatus = LeadStatus.OPEN,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class EntityDraft(
    val name: String,
    val entityType: EntityType,
    val confidence: ConfidenceLevel,
    val aliases: String = "",
    val summary: String,
    val identifier: String
)

data class AttachmentDraft(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val caption: String,
    val attachmentType: AttachmentType,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val capturedAt: Long? = null,
    val deviceModel: String? = null,
    val fileHash: String? = null
)
