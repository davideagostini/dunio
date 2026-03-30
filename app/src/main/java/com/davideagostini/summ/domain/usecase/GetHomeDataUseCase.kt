package com.davideagostini.summ.domain.usecase

import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.domain.model.EntryDisplayItem
import com.davideagostini.summ.domain.model.HomeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetHomeDataUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<HomeState> = combine(
        entryRepository.allEntries,
        categoryRepository.allCategories,
        entryRepository.balance,
    ) { entries, categories, balance ->
        val categoriesBySystemKey = categories.mapNotNull { category ->
            category.systemKey?.let { systemKey -> systemKey to category }
        }.toMap()
        val categoriesByName = categories.associateBy { it.name }
        HomeState(
            entries = entries.map { entry ->
                val matchedCategory =
                    entry.categoryKey?.let(categoriesBySystemKey::get)
                        ?: categoriesByName[entry.category]
                EntryDisplayItem(
                    id          = entry.id,
                    type        = entry.type,
                    description = entry.description,
                    price       = entry.price,
                    category    = matchedCategory?.name ?: entry.category,
                    categoryKey = entry.categoryKey,
                    emoji       = matchedCategory?.emoji ?: "📦",
                    date        = entry.date,
                )
            },
            balance = balance,
        )
    }
}
