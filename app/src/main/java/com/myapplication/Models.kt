package com.myapplication

// ---------------------------------------------------------------
// Models cho đề tài Quản lý món ăn (SQLite thuần)
// Tên bảng / tên cột có tiền tố MSSV_ được khai báo ở AppDatabase.kt
// ---------------------------------------------------------------

data class DishCategory(
    val id: Int = 0,
    val name: String,
    val description: String = ""
)

data class DishImage(
    val id: Int = 0,
    val dishId: Int = 0,
    val imageUrl: String,
    val isThumbnail: Boolean = false
)

data class Dish(
    val id: Int = 0,
    val name: String,
    val price: Double,
    val preparationTime: Int,
    val isAvailable: Boolean = true,
    val description: String = "",
    val dishCategoryId: Int,
    val images: List<DishImage> = emptyList()   // không lưu trong bảng Dish, đọc từ bảng DishImage
) {
    // Quick / Regular tính khi hiển thị, không lưu vào CSDL (theo đề: PreparationTime <= 15 là Quick)
    val isQuick: Boolean get() = preparationTime <= 15

    val thumbnailUrl: String? get() = images.firstOrNull { it.isThumbnail }?.imageUrl
}

/** Mã lỗi validation. UI tự dịch sang English / Tiếng Việt, nên DB không trả chuỗi. */
enum class DishError {
    NAME_EMPTY,
    PRICE_INVALID,
    TIME_INVALID,
    CATEGORY_INVALID,
    NAME_DUPLICATE
}

enum class DeleteResult { DELETED, IN_USE, NOT_FOUND }

enum class SortOption { NONE, PRICE_ASC, PRICE_DESC, TIME_ASC }

/** Gom các điều kiện lọc để dễ giữ lại khi quay về màn hình danh sách. */
data class DishFilter(
    val keyword: String = "",
    val categoryId: Int? = null,
    val onlyAvailable: Boolean = false,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sort: SortOption = SortOption.NONE
) {
    /** false khi khoảng giá không hợp lệ (min > max hoặc giá âm). */
    val isPriceRangeValid: Boolean
        get() {
            val lo = minPrice
            val hi = maxPrice
            if (lo != null && lo < 0) return false
            if (hi != null && hi < 0) return false
            if (lo != null && hi != null && lo > hi) return false
            return true
        }
}
