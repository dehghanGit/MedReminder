package me.sandbad.medireminder.core.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.sqldelight.AppDatabase

class MedicationRepositoryImpl(db: AppDatabase) : MedicationRepository {
    private val queries = db.medicationQueries

    override fun observeActive(): Flow<List<Medication>> =
        queries.selectActive().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toModel() } }

    override fun observeAll(): Flow<List<Medication>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toModel() } }

    override suspend fun getById(id: Long): Medication? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    override suspend fun getAllActive(): List<Medication> = withContext(Dispatchers.Default) {
        queries.selectActive().executeAsList().map { it.toModel() }
    }

    override suspend fun getAll(): List<Medication> = withContext(Dispatchers.Default) {
        queries.selectAll().executeAsList().map { it.toModel() }
    }

    override suspend fun getLowStock(): List<Medication> = withContext(Dispatchers.Default) {
        queries.selectActive().executeAsList().map { it.toModel() }.filter { it.needsRefill }
    }

    override suspend fun insert(medication: Medication): Long = withContext(Dispatchers.Default) {
        queries.transactionWithResult {
            queries.insert(
                name = medication.name,
                form = medication.form.name,
                strength = medication.strength,
                strengthUnit = medication.strengthUnit.name,
                color = medication.color.name,
                instructions = medication.instructions,
                notes = medication.notes,
                stockCount = medication.stockCount,
                refillAt = medication.refillAt,
                startDate = medication.startDate,
                endDate = medication.endDate,
                isArchived = medication.isArchived,
                createdAt = medication.createdAt,
                updatedAt = medication.updatedAt
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun update(medication: Medication) = withContext(Dispatchers.Default) {
        queries.update(
            name = medication.name,
            form = medication.form.name,
            strength = medication.strength,
            strengthUnit = medication.strengthUnit.name,
            color = medication.color.name,
            instructions = medication.instructions,
            notes = medication.notes,
            stockCount = medication.stockCount,
            refillAt = medication.refillAt,
            startDate = medication.startDate,
            endDate = medication.endDate,
            isArchived = medication.isArchived,
            updatedAt = currentDateTime(),
            id = medication.id
        )
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = withContext(Dispatchers.Default) {
        queries.setArchived(archived, currentDateTime(), id)
    }

    override suspend fun adjustStock(id: Long, amount: Double) = withContext(Dispatchers.Default) {
        queries.adjustStock(amount, currentDateTime(), id)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }
}
